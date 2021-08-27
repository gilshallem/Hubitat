/*
 *	Wake On Lan
 *
 *	Author: Ramdev Shallem
 * 
 * 
 * 
 */

metadata {
    definition (name: "WakeOnLan Device", namespace: "ramdev", author: "Ramdev") {
        capability "Momentary"
        capability "Switch"
        command "wake"
    }

    preferences {
        input(name:"myMac", type: "text", required: true, title: "MAC of workstation")
        input(name:"mySecureOn", type: "text", required: false, title: "SecureOn",description:"Certain NICs support a security feature called \"SecureOn\". It allows users to store within the NIC a hexadecimal password of 6 bytes. Example: \"EF4F34A2C43F\"")
        input(name:"myIP", type: "text", required: false, title: "IP Address",description:"Use this for accessing remote computers outside the local LAN. If not entered, will send the packet to all the devices inside the LAN (255.255.255.255)")
        input(name:"myPort", type: "number", required: false, title: "Port",description:"Default: 7",defaultValue :"7")
    }
    
}

def on() {
   wake()
}
def push() {
   wake()
}

def push(n) {
  wake()
}


def wake() {
    def secureOn = mySecureOn ?: "000000000000"
    def port = myPort ?: 7
    def ip = myIP ?: "255.255.255.255"
    def macHEX = myMac.replaceAll("-","").replaceAll(":","").replaceAll(" ","")
    def command = "FFFFFFFFFFFF$macHEX$macHEX$macHEX$macHEX$macHEX$macHEX$macHEX$macHEX$macHEX$macHEX$macHEX$macHEX$macHEX$macHEX$macHEX$macHEX$secureOn"
    def myHubAction = new hubitat.device.HubAction(command, 
                           hubitat.device.Protocol.LAN, 
                           [type: hubitat.device.HubAction.Type.LAN_TYPE_UDPCLIENT, 
                            destinationAddress: "$ip:$port",
                            encoding: hubitat.device.HubAction.Encoding.HEX_STRING])
    sendHubCommand(myHubAction)
    log.info "Sent WOL to $myMac"
    //log.debug "Sent magic packet $command to $ip:$port"
    

}
def updated() {
   device.updateDataValue("MAC",myMac)
   device.updateDataValue("SecureOn",mySecureOn)
   device.updateDataValue("IP",myIP)
   device.updateDataValue("Port",myPort ? "$myPort":"")
    
   
}

def configure(){
    sendEvent(name: "numberOfButtons", value: 1)
}