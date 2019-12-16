// Original Tasmota driver developed by Brett Sheleski
// Ported to Hubitat by dkkohler
// HSB Color Settings by Eric Maycock (erocm123)
// Modified to work with the tasmota firmware for RGBW LED Light Bulbs using the SYF05 Configuration by damondins
// Hubitat driver for controlling the tasmota firmware for RGBW LED Addressable Lights
// Version 1.0.3 - 9/3/2019 - Added Support for LED Schemes and Speed
// Version 1.0.2 Added Support for Level Duration - Duration can be set from 0 to 7
// Version 1.0.1 Fixed capturing the CT commands from Groups & Scenes when asking for a white color
// Version 1.0.0

metadata {
    definition(name: "nniehoff rpi rgw controller", namespace: "nniehoff", author: "nniehoff") {
        capability "Switch"
        capability "ColorControl"
        capability "ColorTemperature"
        capability "Light"
        capability "LightEffects"
        capability "Switch"
        capability "SwitchLevel"
        
        //command "white"
        //command "ModeNext"
        //command "ModePrevious"
        //command "ModeSingleColor"
        //command "ModeWakeUp"
        //command "ModeCycleUpColors"
        //command "ModeCycleDownColors"
        //command "ModeRandomColors"
        //command "ModeClock"
        //command "ModeCandleLight"
        //command "ModeRGB"
        command "ModeChristmas"
        //command "ModeHannukah"
        //command "ModeKwanzaa"
        //command "ModeRainbow"
        //command "ModeFire"
        //command "SetSpeed",               ["number 0 - 20"]
    }

    preferences {       
        section("Host") {
            input(name: "ipAddress", type: "string", title: "IP Address", displayDuringSetup: true, required: true)
            input(name: "port", type: "number", title: "Port", displayDuringSetup: true, required: true, defaultValue: 8000)
        }

        section("Authentication") {
            input(name: "username", type: "string", title: "Username", displayDuringSetup: false, required: false)
            input(name: "password", type: "password", title: "Password (sent cleartext)", displayDuringSetup: false, required: false)
        }

        section("Settings") {
            //input(name: "fade", type: "bool", title: "Fade", displayDuringSetup: false, required: false, defaultValue: false)
        }
    }
}

def parse(String description) {
    def message = parseLanMessage(description)
    def isParsed = false;

    // parse result from current formats
    def resultJson = {}
    if (message?.json) {
        // current json data format
        resultJson = message.json
        log.debug resultJson
    }  
}

def on() {
    sendEvent(name: "switch", value: "on")
    sendCommand("Power", "power=on")
}

def off() {
    sendEvent(name: "switch", value: "off")
    sendCommand("Power", "power=off")
}

def setColorTemperature(value=2700)
{
    sendEvent(name: "colorTemperature", value: value)
    log.debug("Unable to change color temperature")
    setHsv(0,0,state.level)
}    
    
def setColor(value) {
    log.debug "HSVColor = "+ value
       if (value instanceof Map) {
        def h = value.containsKey("hue") ? value.hue : null
        def s = value.containsKey("saturation") ? value.saturation : null
        def v = value.containsKey("level") ? value.level : null
        setHsv(h, s, v)
    } else {
        log.warn "Invalid argument for setColor: ${value}"
    }
}


def setHsv(h,s,v)
{
    log.debug("setHSV - ${h},${s},${v}")
    state.hue = h
    state.saturation = s
    state.level = v
    state.colorMode = "RGB"
    sendEvent(name: "hue", value: "${h}")
    sendEvent(name: "saturation", value: "${s}")
    sendEvent(name: "level", value: "${v}")

    if (h == 0 && s == 0) {
        state.colorMode = "CT"
        sendEvent(name: "colorMode", value: "CT")
        red = 0
        green = 0
        blue = 0
        white = 2.55*v
        }
    else {
        state.colorMode = "RGB"
        sendEvent(name: "colorMode", value: "RGB")
        rgbColors = hubitat.helper.ColorUtils.hsvToRGB( [h, s, v] )
        red = rgbColors[0].toInteger()
        green = rgbColors[1].toInteger()
        blue = rgbColors[2].toInteger()
        white = 0
    }
    rgbcmd = "r=" + red + "&g=" + green + "&b=" + blue + "&w=" + white.toInteger()
    sendCommand("color", rgbcmd)
}

def setHue(h)
{
    setHsv(h, state.saturation, state.level)
}

def setSaturation(s)
{
    setHsv(state.hue, s, state.level)
}

def setLevel(v)
{
    setHsv(state.hue, state.saturation, v)
}

def setPreviousEffect() {

}

def setNextEffect() {

}

Map lightEffects = [1:"Fire Place",2:"Storm",3:"Deep Fade",4:"Lite Fade",5:"Police"]

def installed() {
    def le = new groovy.json.JsonBuilder(lightEffects)
    sendEvent(name:"lightEffects",value:le)
}

def setEffect(String effect){
    def id = lightEffects.find{ it.value == effect }
    if (id) setEffect(id.key)
}

def setEffect(id){
    def descriptionText
    def efSelect = lightEffects."${id}"
    descriptionText = "${device.displayName}, effect was was set to ${efSelect}"
    if (txtEnable) log.info "${descriptionText}"
    sendEvent(name:"effectName", value:efSelect, descriptionText:descriptionText)
    descriptionText = "${device.displayName}, colorMode is EFFECTS"
    if (txtEnable) log.info "${descriptionText}"
    sendEvent(name:"colorMode", value:"EFFECTS", descriptionText:descriptionText)
    state.crntEffectId = id
    //device specific code here
}


// def ModeNext() {
//     if (state.mode < 12) {
//         state.mode = state.mode + 1
//     }
//     else {
//         state.mode = 0
//     }
//     sendEvent(name: "mode", value: "${state.mode}")
//     sendCommand("Scheme", "${state.mode}")
// }

// def ModePrevious() {
//     if (state.mode > 0) {
//         state.mode = state.mode - 1
//     }
//     else {
//         state.mode = 12
//     }
//     sendEvent(name: "mode", value: "${state.mode}")
//     sendCommand("Scheme", "${state.mode}")
// }

// def ModeSingleColor() {
//     state.mode = 0
//     sendEvent(name: "mode", value: "${state.mode}")
//     sendCommand("Scheme", "0")
// }

// def ModeWakeUp() {
//     state.mode = 1
//     sendEvent(name: "mode", value: "${state.mode}")
//     sendCommand("Scheme", "1")
// }

// def ModeCycleUpColors() {
//     state.mode = 2
//     sendEvent(name: "mode", value: "${state.mode}")
//     sendCommand("Scheme", "2")
// }

// def ModeCycleDownColors() {
//     state.mode = 3
//     sendEvent(name: "mode", value: "${state.mode}")
//     sendCommand("Scheme", "3")
// }

// def ModeRandomColors() {
//     state.mode = 4
//     sendEvent(name: "mode", value: "${state.mode}")
//     sendCommand("Scheme", "4")
// }

// def ModeClock() {
//     state.mode = 5
//     sendEvent(name: "mode", value: "${state.mode}")
//     sendCommand("Scheme", "5")
// }

// def ModeCandleLight() {
//     state.mode = 6
//     sendEvent(name: "mode", value: "${state.mode}")
//     sendCommand("Scheme", "6")
// }

// def ModeRGB() {
//     state.mode = 7
//     sendEvent(name: "mode", value: "${state.mode}")
//     sendCommand("Scheme", "7")
// }

def ModeChristmas() {
    sendEvent(name:"effectName", value:"Christmas", descriptionText:"Christmas Lights")
    sendCommand("effects", "effect=christmas")
}

// def ModeHannukah() {
//     state.mode = 9
//     sendEvent(name: "mode", value: "${state.mode}")
//     sendCommand("Scheme", "9")
// }

// def ModeKwanzaa() {
//     state.mode = 10
//     sendEvent(name: "mode", value: "${state.mode}")
//     sendCommand("Scheme", "10")
// }

// def ModeRainbow() {
//     state.mode = 11
//     sendEvent(name: "mode", value: "${state.mode}")
//     sendCommand("Scheme", "11")
// }

// def ModeFire() {
//     state.mode = 12
//     sendEvent(name: "mode", value: "${state.mode}")
//     sendCommand("Scheme", "12")
// }

// def SetSpeed(speed) {
//     def ispeed = speed.toInteger()
//     if (ispeed >= 0 && ispeed <= 20) {
//         sendEvent(name: "speed", value: "${speed}")
//         sendCommand("Speed", "${speed}")
//     }
// }

private def sendCommand(String command, payload) {
    sendCommand(command, payload.toString())
}

private def sendCommand(String command, String payload) {
    //log.debug "sendCommand(${command}:${payload}) to device at $ipAddress:$port"

    if (!ipAddress || !port) {
        log.warn "aborting. ip address or port of device not set"
        return null;
    }
    def hosthex = convertIPtoHex(ipAddress)
    def porthex = convertPortToHex(port)
    //String basicauth = "${username}:${password}".bytes.encodeBase64().toString()

    def path = ""
    if (payload){
        path += "?cmnd=${command}&${payload}" //&nick=${basicauth}"
    }
    else{
        path += "?cmnd=${command}" //&nick=${basicauth}"
    }

    //if (username){
    //    path += "&user=${username}"
    //    if (password){
    //        path += "&password=${password}"
    //    }
    //}

    log.debug "http://${ipAddress}:${port}/${path}"
    def result = new hubitat.device.HubAction(
        method: "GET",
        path: path,
        headers: [
            HOST: "${ipAddress}:${port}",
            Authorization: "Basic ${basicauth}"
        ]
    )

    return result
}

private String convertIPtoHex(ipAddress) { 
    String hex = ipAddress.tokenize( '.' ).collect {  String.format( '%02x', it.toInteger() ) }.join()
    return hex
}

private String convertPortToHex(port) {
    String hexport = port.toString().format('%04x', port.toInteger())
    return hexport
}