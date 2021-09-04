/*
 *	PC Controller
 *
 *	Author: Ramdev Shallem
 * 
 * 
 * 
 */

import hubitat.helper.InterfaceUtils
import groovy.transform.Field


@Field static Map connected =[:]
@Field static Map sentEventTime=[:]
@Field static Map eventAfterConnect=[:]
@Field static Map pluginVersion=[:]


metadata {
    definition (name: "PC Controller Device", namespace: "ramdev", author: "Ramdev") {
        capability "PushableButton"
        capability "Initialize"
        command "SendEvent" ,[[name:"Event Data*", type: "STRING", description: "Enter event data", required: true ]] 
        command "WakeupAndSendEvent" ,[[name:"Event Data*", type: "STRING", description: "Enter event data", required: true ]] 
        command "WakeOnLan"
        command "Connect"
        command "AddStringParam",[[name:"Value*", type: "STRING", description: "Enter value", required: true ]]
        command "AddNumberParam",[[name:"Value*", type: "NUMBER", description: "Enter value", required: true ]]
        command "ClearParams"
        attribute "Connected", "boolean"
        attribute "EG Plugin Version", "string"
        attribute "EG Plugin Outdated", "boolean"
        attribute "ReceivedCommand", "string"
        attribute "ReceivedParam1", "string"
        attribute "ReceivedParam2", "string"
        attribute "ReceivedParam3", "string"
        attribute "ReceivedNumParam1", "number"
        attribute "ReceivedNumParam2", "number"
        attribute "ReceivedNumParam3", "number"
    }

    preferences {
        input(name:"myIP", type: "text", required: true, title: "IP Address",description:"Computer IP")
        input(name:"myPort", type: "number", required: true, title: "Port",description:"Port",defaultValue :"345")
        input(name:"myUsername", type: "text", required: false, title: "User Name")
        input(name:"myPassword", type: "text", required: false, title: "Password")
        
        
        input(name:"myWOLMac", type: "text", required: false, title: "[Wake On Lan] MAC of workstation",description:"If left empty, will get the MAC from the IP address")
        input(name:"mySecureOn", type: "text", required: false, title: "[Wake On Lan] SecureOn",description:"Certain NICs support a security feature called \"SecureOn\". It allows users to store within the NIC a hexadecimal password of 6 bytes. Example: \"EF4F34A2C43F\"")
        input(name:"myWOLPort", type: "number", required: false, title: "[Wake On Lan] Port",description:"Default: 7",defaultValue :"7")
        
        input(name:"myWakeupTime", type: "number", required: false, title: "[Wake On Lan] Max wakeup time (secs)",description:"The maximum time in seconds it takes for the computer to wakeup (after that any event that was triggered while the pc was off will be forgotten)",defaultValue :90)
        input(name:"myReconnectDelay", type: "number", required: false, title: "Reconnect Delay (secs)",description:"The interval between each reconnect attempt in seconds",defaultValue :1)
        
    }
    
}

def initialize() {
    state.remove("sentEventTime")
    state.remove("connected")
    state.remove("eventAfterConnect")
    connected[device.id] = false
    Connect()
}


def updated() {
    device.updateDataValue("IP",myIP ? "$myIP":"")
    device.updateDataValue("Port",myPort ? "$myPort":"345")
    device.updateDataValue("Username",myUsername ? "$myUsername":"")
    device.updateDataValue("Password",myPassword ? "$myPassword":"")
    if (!connected[device.id]) {
        Connect()
    } 
    else {
        //reconnect
        interfaces.webSocket.close()
    }
    
    
}

def installed() {
    Connect()
}
def Connect() {
    
    if (!connected[device.id]) {
        def uri = "http://$myIP:$myPort"
        if (myUsername?.trim() && myPassword?.trim()) {
            interfaces.webSocket.connect(uri,pingInterval: 20,headers:["Authorization": "Basic " +(myUsername + ":" + myPassword).bytes.encodeBase64()])
        }
        else {
            interfaces.webSocket.connect(uri,pingInterval: 20)
        }
    }
}
def AddNumberParam(value) {
    addParam(value)
}
def AddStringParam(value) {
    addParam(value)
   
}
def ClearParams() {
    state.remove("sendParams")
    
}
def addParam(value) {
    def params =  state["sendParams"]
    if (!params) {
       params = []; 
    }
    if (value.class == String) {
        params.add("\"" + value +"\"")
    }
    else {
        params.add(value)
    }
    
    state["sendParams"] = params
}

def parse(String cmd) {
    log.debug "recived: $cmd"
    def json = parseJson(cmd);
    if (json[0] == "plugin_version") {
        pluginVersion[device.id] = [json[1],json[2]]
        findLatestVersion()
    }
    else {
        sendEvent(name: "ReceivedCommand", value: json[0],isStateChange: true)
        setReceivedParams(json)
    }

}
def comparePluginVersion(newVer) {
    def verNum = newVer[0]
    
    if (pluginVersion[device.id]) {
        def outdated = pluginVersion[device.id][1]<verNum   
        if (outdated) {
            sendEvent(name: "EG Plugin Version", value: pluginVersion[device.id][0] + " (outdated, please update to " + newVer[1] +")")
            sendEvent(name: "EG Plugin Outdated", value:true)
        }
        else {
            sendEvent(name: "EG Plugin Version", value: pluginVersion[device.id][0] + " (Latest version)")
            sendEvent(name: "EG Plugin Outdated", value:false)
        }
         
    } 
    else {
        sendEvent(name: "EG Plugin Version", value: "1.0.1 (outdated, please update to " + newVer[1] +")")
        sendEvent(name: "EG Plugin Outdated", value:true)
    }
    
}

def checkIfReceivedPluginVersion() {
    if (!pluginVersion[device.id]) {
        findLatestVersion()
    }
}
def onRecieveVersion(resp,deta) {
    if (resp.status) {
        comparePluginVersion(resp.getJson())
    }
}
def findLatestVersion() {
    asynchttpGet(onRecieveVersion,[uri:"https://raw.githubusercontent.com/gilshallem/Hubitat/main/ComputerController/EventGhostPlugin/version.json"]) 
}

def setReceivedParams(json) {
    def paramCounter =1
    def numParamCounter =1
    def dblParamCounter =1
    json.eachWithIndex{ v,i ->
        if (v) {
            if (i>0) {
                if (v.isNumber()) {
                   if (numParamCounter<=3) {
                       sendEvent(name: "ReceivedNumParam" + numParamCounter, value: v,isStateChange: true)
                       numParamCounter++
                   }
                } else {
                   if (paramCounter<=3) {
                       sendEvent(name: "ReceivedParam" + paramCounter, value: v,isStateChange: true)
                       paramCounter++
                   }
                    
                }
            }
        }
    }
    for(i in paramCounter..3) {
       device.deleteCurrentState("ReceivedParam" + i)
    }
    for(i in numParamCounter..3) {
       device.deleteCurrentState("ReceivedNumParam" + i)
    }
}

def httpcallback() {
    
}

def SendEvent(eventName) {
    def params = state["sendParams"];
    if (!params) params=[]
    log.debug "sending " + eventName
    interfaces.webSocket.sendMessage("{\"event\":\"" +eventName +"\",\"params\":"+ state["sendParams"] +"}" )
    ClearParams() 
}
def WakeupAndSendEvent(event) {
    if (!connected[device.id]) {
        eventAfterConnect[device.id] = event
        sentEventTime[device.id] = now()
        WakeOnLan()    
    }
    else {
        SendEvent(event)
    }
}

def WakeOnLan() {
    def secureOn = mySecureOn ?: "000000000000"
    def port = myWOLPort ?: 7
    def ip = "255.255.255.255"
    def myMac = myWOLMac ?: getMACFromIP(myIP);
    def macHEX = myMac.replaceAll("-","").replaceAll(":","").replaceAll(" ","")
    def command = "FFFFFFFFFFFF$macHEX$macHEX$macHEX$macHEX$macHEX$macHEX$macHEX$macHEX$macHEX$macHEX$macHEX$macHEX$macHEX$macHEX$macHEX$macHEX$secureOn"
    def myHubAction = new hubitat.device.HubAction(command, 
                           hubitat.device.Protocol.LAN, 
                           [type: hubitat.device.HubAction.Type.LAN_TYPE_UDPCLIENT, 
                            destinationAddress: "$ip:$port",
                            encoding: hubitat.device.HubAction.Encoding.HEX_STRING])
    sendHubCommand(myHubAction)
    log.info "Sent WOL to $myMac"
}

def webSocketStatus(String status){
    
    def logEnable = true
    if (logEnable) log.debug "webSocketStatus- ${status}"

    if(status.startsWith('failure: ')) {
        connected[device.id] = false
        sendEvent(name: "Connected", value: false)
        log.warn("failure message from web socket ${status}")
 
       reconnect()
    } 
    else if(status == 'status: open') {
        log.info "websocket is open"
        sendEvent(name: "Connected", value: true)
        connected[device.id] = true
        onConnected()
      
    } 
    else if (status == "status: closing"){
        sendEvent(name: "Connected", value: false)
        connected[device.id] = false
        log.warn "WebSocket connection closing."
         reconnect()
       
    } 
    else {
        sendEvent(name: "Connected", value: false)
        connected[device.id] = false
        log.warn "WebSocket error, reconnecting."
        reconnect()
    
    }
}

def onConnected() {
    runIn(5,"checkIfReceivedPluginVersion")
    if (eventAfterConnect[device.id]) {
        if (now() - sentEventTime[device.id] < (myWakeupTime?:90) *1000) {
            SendEvent(eventAfterConnect[device.id])
            eventAfterConnect[device.id]=null
        }
    }
}
def reconnect() {
    runIn(myReconnectDelay?:1,"Connect")
}


def push() {}




