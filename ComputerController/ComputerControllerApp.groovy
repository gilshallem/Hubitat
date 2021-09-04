/*
 *	Computer Controller
 *
 *	Author: Ramdev Shallem
 * 
 * 
 * 
 */
public static String version()      {  return "v1.02"  }
definition(
    name: "Computer Controller (By Ramdev)",
    namespace: "ramdev",
    author: "Ramdev",
    description: "Connects your PC and the hubitat hub. requires EventGhost",
	singleInstance: true,
    category: "Utilities",
    iconUrl: "",
    iconX2Url: "",
    iconX3Url: "",
	documentationLink: "https://community.hubitat.com/t/78640"
)


preferences 
{
    page(name: "MainPage")
    page(name: "configureDevicePage")
    page(name: "pageRemove")

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
	log.debug "ComputerControllerApp ${version()} - Initialized"
}


def MainPage() {
	if (!state.AppIsInstalled) {
		return dynamicPage(name: "MainPage", title: "", install:true, uninstall: true){
			section("<h2>Computer Controller (by Ramdev)</h2>") {
				paragraph ""
				paragraph"This software is provided \"AS IS\", without warranty of any kind, express or implied, including but not limited to the warranties of merchantability, fitness for a particular purpose and noninfringement."
				paragraph ""
				paragraph "<b>Before you start you will need to download and install EventGhost with the hubitat plugin on your pc.</b><br>Read more here: <a href='https://community.hubitat.com/t/78640' target='newwin'>https://community.hubitat.com/t/78640</a>"
                paragraph ""
				paragraph "<h2>To complete the installation, click on \"Done\"</h2>"
			}
		}
	}
	
    if (state.configuringDevice) configureDevice();
	return dynamicPage(name: "MainPage", install:true, uninstall: false){
        if (state.configuringDevice ) { configureDevice() }
        
        section("<h2>Computer Controller ${version()} (by Ramdev)</h2>"){
			def devCount = 0
            paragraph "Computers:"
			getChildDevices().sort({ a, b -> a.label <=> b.label }).each{
				devCount = devCount + 1
                href (name: "configureDevicePage", title: "💻 ${it.label}",
					  description: "Click to configure",
                       params: [deviceNetworkId: it.deviceNetworkId, deviceName: it.label],
					   page: "configureDevicePage")
				
			}
            href (name: "configureDevicePage", title: "➕  Add a Computer",
                  description: "Click to add a new Computer",
					  page: "configureDevicePage")
            paragraph "Help:"
            href "", title: "📖 Learn How To Configure And Use", style: "external", url: "https://community.hubitat.com/t/78640", description: "Click to view the tutorial", required: false
            paragraph "Uninstall:"
			href "pageRemove", title: "⛔ Uninstall The App", description: "Click to uninstall Computer Controller"
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
		    section("<h1>Add A New Computer</h1>"){
		    	input "configureDeviceName", "text", title: "Device Name", required: true, multiple: false, submitOnChange: false
                input "configureIP", "text", title: "IP Address (Static)",description:"Static Ip address of the computer", required: true, multiple: false, submitOnChange: false
                input "configurePort", "text",title: "Port",description:"Default: 345", required: true, multiple: false, submitOnChange: false, defaultValue:345
                input "configureUsername", "text", title: "Username", required: false, multiple: false, submitOnChange: false
                input "configurePassword", "text", title: "Password", required: false, multiple: false, submitOnChange: false
			
		    }
	    }
    } else {
         state.editedId = params.deviceNetworkId;
        def device = getChildDevice( state.editedId)
         dynamicPage(name: "configureDevicePage", title: ""){
		    section("<h1>Configure Computer</h1>"){
                input "configureDeviceName", "text", title: "Device Name", required: true, multiple: false, submitOnChange: false, defaultValue: device.label
                input "configureIP", "text", title: "IP Address (Static)",description:"Static Ip address of the computer", required: true, multiple: false, submitOnChange: false, defaultValue: device.getDataValue("IP")
                input "configurePort", "number",title: "Port",description:"Default: 345", required: false, multiple: false, submitOnChange: false, defaultValue: device.getDataValue("Port").toInteger() 
                input "configureUsername", "text", title: "Username", required: false, multiple: false, submitOnChange: false, defaultValue: device.getDataValue("Username")
                input "configurePassword", "text", title: "Password", required: false, multiple: false, submitOnChange: false, defaultValue: device.getDataValue("Password")
			    input "removeDeviceBTN", "button", title: "Remove Device"
		    }
	    }
        
    }
	
}

def pageRemove(){
	dynamicPage(name: "pageRemove", title: "", install: false, uninstall: true){
		section('CAUTION'){
			paragraph "You are about to completely remove Computer Controller and all of the computers.", required: true
			paragraph "If you are sure you want to do this, please tap on the Remove button below, To cancel press Done.", required: true
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
    app.clearSetting("configureIP")
    app.clearSetting("configurePort")
    app.clearSetting("configurePassword")
    app.clearSetting("configureUsername")
}
def configureDevice() {
	if (state.editedId==null) {
		try {
			def newDevice = addChildDevice(
				"ramdev",
				"Computer Controller",
				"PC" + now(),
				[
					"label" : configureDeviceName,
               
					isComponent: true
				]
			)
            newDevice.updateSetting("myPort",[type:"number", value: configurePort])
            newDevice.updateSetting("myIP",[type:"text", value: configureIP])
            newDevice.updateSetting("myPassword",[type:"text", value: configurePassword])
            newDevice.updateSetting("myUsername",[type:"text", value: configureUsername])
			newDevice.updated()

		} catch (error) {

		}
	} else {
        def device = getChildDevice(state.editedId)
        device.updateSetting("myPort",[type:"number", value: configurePort?:345])
        device.updateSetting("myIP",[type:"text", value: configureIP?:""])
        device.updateSetting("myPassword",[type:"text", value: configurePassword?:""])
        device.updateSetting("myUsername",[type:"text", value: configureUsername?:""])
        device.label = configureDeviceName
		device.updated()
	}
	state.configuringDevice = false
	state.configuringDeviceData = null
    state.editedId=null;
	clearDeviceConfigSettings()
	return MainPage()
}




