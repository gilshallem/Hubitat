/*
 *	Wake On Lan
 *
 *	Author: Ramdev Shallem
 * 
 * 
 * 
 */
public static String version()      {  return "v1.01"  }
definition(
    name: "WakeOnLan (By Ramdev)",
    namespace: "ramdev",
    author: "Ramdev",
    description: "Wake up PCs",
	singleInstance: true,
    category: "Utilities",
    iconUrl: "",
    iconX2Url: "",
    iconX3Url: "",
	documentationLink: "https://community.hubitat.com/t/release-wake-on-lan-wake-up-your-computers-via-the-hub/78362"
)


preferences 
{
    page(name: "MainPage")
    page(name: "configureDevicePage")

}

def installed() 
{
	
    state.AppIsInstalled = true
	initialize()
}

def updated() 
{
	

	initialize()
}

def initialize() 
{
    state.configuringDevice = false
	clearDeviceConfigSettings()
    state.editedId = null;
    state.deleted= false;
	log.debug "WakeOnLan ${version()} - Initialized"
}


def MainPage() {
	if (!state.AppIsInstalled) {
		return dynamicPage(name: "MainPage", title: "", install:true, uninstall: true){
			section("<h2>WakeOnLan</h2>") {
				paragraph ""
				paragraph"This software is provided \"AS IS\", without warranty of any kind, express or implied, including but not limited to the warranties of merchantability, fitness for a particular purpose and noninfringement."
				paragraph ""
				paragraph "<h2>To complete the installation, click on \"Done\"</h2>"
			}
		}
	}
	
    if (state.configuringDevice) configureDevice();
	return dynamicPage(name: "MainPage", title: "WakeOnLan ${version()}", install:true, uninstall: true){
        if (state.configuringDevice ) { configureDevice() }
        
        section("<h2>WOL Devices</h2>"){
			def devCount = 0
			getChildDevices().sort({ a, b -> a.label <=> b.label }).each{
				
					devCount = devCount + 1
                    def mac=it.getDataValue("MAC")
                href (name: "configureDevicePage", title: "${it.label} [${mac}]",
					  description: "Click to configure",
                       params: [deviceNetworkId: it.deviceNetworkId, deviceName: it.label],
					   page: "configureDevicePage")
				
			}
            href (name: "configureDevicePage", title: "<font size='5'>➕</font>  Add Device",
                  description: "Click to add a new WOL Device",
					  page: "configureDevicePage")
			
		}
		
	}
}

def configureDevicePage(params) {
    if (state.deleted) {
         state.deleted = false;
        return MainPage();
    }
    state.configuringDevice = true
	clearDeviceConfigSettings()
    if (params==null) {
        dynamicPage(name: "configureDevicePage", title: ""){
		    section("<h1>Configure a device</h1>"){
		    	input "configureDeviceName", "text", title: "Device Name", required: true, multiple: false, submitOnChange: false
                input "configureMACAddress", "text", title: "MAC Address", required: true, multiple: false, submitOnChange: false
                input "configureIP", "text", title: "IP Address",description:"Use this for accessing remote computers outside the local LAN. If not entered, will send the packet to all the devices inside the LAN (255.255.255.255)", required: false, multiple: false, submitOnChange: false
                input "configurePort", "text",title: "Port",description:"Default: 7", required: false, multiple: false, submitOnChange: false
                input "configureSecureOn", "text", title: "SecureOn",description:"Certain NICs support a security feature called \"SecureOn\". It allows users to store within the NIC a hexadecimal password of 6 bytes. Example: \"EF4F34A2C43F\"", required: false, multiple: false, submitOnChange: false
			
		    }
	    }
    } else {
         state.editedId = params.deviceNetworkId;
        def device = getChildDevice( state.editedId)
         dynamicPage(name: "configureDevicePage", title: ""){
		    section("<h1>Configure a device</h1>"){
		    	input "configureDeviceName", "text", title: "Device Name", required: true, multiple: false, defaultValue: device.label, submitOnChange: false
                input "configureMACAddress", "text", title: "MAC Address", required: true, multiple: false, defaultValue: device.getDataValue("MAC"), submitOnChange: false
                input "configureIP", "text", title: "IP Address",description:"Use this for accessing remote computers outside the local LAN. If not entered, will send the packet to all the devices inside the lan (255.255.255.255)", required: false, multiple: false, submitOnChange: false, defaultValue: device.getDataValue("IP")
                input "configurePort", "text",title: "Port",description:"Default: 7", required: false, multiple: false, submitOnChange: false ,defaultValue: device.getDataValue("Port")
                input "configureSecureOn", "text", title: "SecureOn",description:"Certain NICs support a security feature called \"SecureOn\". It allows users to store within the NIC a hexadecimal password of 6 bytes. Example: \"EF4F34A2C43F", required: false, multiple: false, submitOnChange: false, defaultValue: device.getDataValue("SecureOn")
			    input "removeDeviceBTN", "button", title: "Remove Device"
		    }
	    }
        
    }
	
}
def appButtonHandler(BTN) {
	if (BTN == "removeDeviceBTN") {
		deleteChildDevice(state.editedId)
		clearDeviceConfigSettings()
        state.configuringDevice = false
    state.editedId=null;
        state.deleted = true;
    }
}

def clearDeviceConfigSettings() {
    app.clearSetting("configureDeviceName")
    app.clearSetting("configureMACAddress")
    app.clearSetting("configureIP")
    app.clearSetting("configurePort")
    app.clearSetting("configureSecureOn")
}
def configureDevice() {
	if (state.editedId==null) {
		try {
			def newDevice = addChildDevice(
				"ramdev",
				"WakeOnLan Device",
				"WOL" + now(),
				[
					"label" : configureDeviceName,
               
					isComponent: true
				]
			)
			newDevice.updateSetting("myMac",[type:"text", value: configureMACAddress])
            newDevice.updateSetting("myPort",[type:"text", value: configurePort])
            newDevice.updateSetting("myIP",[type:"text", value: configureIP])
            newDevice.updateSetting("mySecureOn",[type:"text", value: configureSecureOn])
			newDevice.updated()

		} catch (error) {

		}
	} else {
        def device = getChildDevice(state.editedId)
        device.updateSetting("myMac",[type:"text", value: configureMACAddress])
        device.updateSetting("myPort",[type:"text", value: configurePort?:""])
        device.updateSetting("myIP",[type:"text", value: configureIP?:""])
        device.updateSetting("mySecureOn",[type:"text", value: configureSecureOn?:""])
        device.label = configureDeviceName
		device.updated()
	}
	state.configuringDevice = false
	state.configuringDeviceData = null
    state.editedId=null;
	clearDeviceConfigSettings()
	return MainPage()
}

