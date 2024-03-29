#!/usr/bin/env python3

from http.server import HTTPServer, BaseHTTPRequestHandler
from io import BytesIO
from rpi_ws281x import Color, PixelStrip, ws
from urllib.parse import urlparse
from urllib.parse import parse_qs
import math
from random import seed
from random import randint

# LED strip configuration:
LED_COUNT      = 253      # Number of LED pixels.
LED_PIN        = 10      # GPIO pin connected to the pixels (must support PWM!).
LED_FREQ_HZ    = 800000  # LED signal frequency in hertz (usually 800khz)
LED_DMA        = 10      # DMA channel to use for generating signal (try 10)
LED_BRIGHTNESS = 255     # Set to 0 for darkest and 255 for brightest
LED_INVERT     = False   # True to invert the signal (when using NPN transistor level shift)
LED_CHANNEL    = 0
LED_STRIP      = ws.SK6812_STRIP_RGBW

class SimpleHTTPRequestHandler(BaseHTTPRequestHandler):
    last_red = 0
    last_green = 0
    last_blue = 0
    last_white = 128

    def do_GET(self):
        self.send_response(200)
        self.end_headers()
        self.wfile.write(b'Hello, world!')
        query_components = parse_qs(urlparse(self.path).query)
        if query_components['cmnd'][0] == 'Power':
            if query_components['power'][0] == 'off':
                allOneColor(strip, Color(0,0,0,0))
            if query_components['power'][0] == 'on':
                allOneColor(strip, Color(self.last_green, self.last_red, self.last_blue, self.last_white))
        if query_components['cmnd'][0] == 'color':
            self.last_red = int(query_components['r'][0])
            self.last_green = int(query_components['g'][0])
            self.last_blue = int(query_components['b'][0])
            self.last_white = int(query_components['w'][0])
            allOneColor(strip, Color(self.last_green, self.last_red, self.last_blue, self.last_white))
        if query_components['cmnd'][0] == 'effects':
            if query_components['effect'][0] == 'christmas':
                setChristmasLights(strip)

def allOneColor(strip, color):
    """Set all LEDs to the same color"""
    for i in range(strip.numPixels()):
        strip.setPixelColor(i, color)
    strip.show()

def setChristmasLights(strip):
    """Set Christmas Colors"""
    christmas_color = [
        Color(255, 0, 0, 0), # Green
        Color(0, 255, 0, 0), # Red
        Color(0, 0, 255, 0), # Blue
        Color(255, 255, 0, 0), # Orange-Yellow
        Color(24, 248, 148, 0) # Pink
    ]

    skip_number = 9
    number_on = 3

    key = randint(0, len(christmas_color)-1)
    for i in range(strip.numPixels()):
        if i % skip_number < number_on:
            strip.setPixelColor(i, christmas_color[key])
        else:
            key = randint(0, len(christmas_color)-1)
            strip.setPixelColor(i, Color(0, 0, 0, 0))
    strip.show()

# seed random number generator
seed(1)

strip = PixelStrip(LED_COUNT, LED_PIN, LED_FREQ_HZ, LED_DMA, LED_INVERT, LED_BRIGHTNESS, LED_CHANNEL, LED_STRIP)
strip.begin()

httpd = HTTPServer(('0.0.0.0', 8000), SimpleHTTPRequestHandler)
httpd.serve_forever()
