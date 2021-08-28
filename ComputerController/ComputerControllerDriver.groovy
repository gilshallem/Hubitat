/*
 *	Computer Controller
 *
 *	Author: Ramdev Shallem
 * 
 * 
 * 
 */

import hubitat.helper.InterfaceUtils


metadata {
    definition (name: "Computer Controller", namespace: "ramdev", author: "Ramdev") {
        capability "PushableButton"
        command "SendEvent" ,[[name:"Event Data*", type: "STRING", description: "Enter event data", required: true ]] 
        command "WakeupAndSendEvent" ,[[name:"Event Data*", type: "STRING", description: "Enter event data", required: true ]] 
        command "WakeOnLan"
        command "Connect"
        command "AddStringParam",[[name:"Value*", type: "STRING", description: "Enter value", required: true ]]
        command "AddNumberParam",[[name:"Value*", type: "NUMBER", description: "Enter value", required: true ]]
        command "ClearParams"
        attribute "Connected", "boolean"
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
  
    connect()
    
}

def updated() {
    device.updateDataValue("IP",myIP ? "$myIP":"")
    device.updateDataValue("Port",myPort ? "$myPort":"345")
    device.updateDataValue("Username",myUsername ? "$myUsername":"")
    device.updateDataValue("Password",myPassword ? "$myPassword":"")
    Connect()
    
}
def Connect() {
    if (!state.connected) {
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
    sendEvent(name: "ReceivedCommand", value: json[0],isStateChange: true)
  
    setReceivedParams(json)
   
       
    
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
    interfaces.webSocket.sendMessage("{\"event\":\"" +eventName +"\",\"params\":"+ state["sendParams"] +"}" )
    ClearParams() 
}
def WakeupAndSendEvent(event) {
    if (!state.connected) {
        state.eventAfterConnect = event
        state.sentEventTime = now()
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
        state.connected = false
        sendEvent(name: "Connected", value: false)
        log.warn("failure message from web socket ${status}")
 
       reconnect()
    } 
    else if(status == 'status: open') {
        log.info "websocket is open"
        sendEvent(name: "Connected", value: true)
        state.connected = true
        onConnected()
      
    } 
    else if (status == "status: closing"){
        sendEvent(name: "Connected", value: false)
        state.connected = false
        log.warn "WebSocket connection closing."
         reconnect()
       
    } 
    else {
        sendEvent(name: "Connected", value: false)
        state.connected = false
        log.warn "WebSocket error, reconnecting."
        reconnect()
    
    }
}

def onConnected() {
    if (state.eventAfterConnect) {
        if (now() - state.sentEventTime < (myWakeupTime?:90) *1000) {
            SendEvent(state.eventAfterConnect)
            state.eventAfterConnect=null
        }
    }
}
def reconnect() {
    runIn(myReconnectDelay?:1,"Connect")
}
def configure(){
    sendEvent(name: "numberOfButtons", value: 1)
}

def push() {}



