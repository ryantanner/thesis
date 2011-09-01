#!/usr/bin/python
###############################################################################
# Copyright (C) 2008 Johann Haarhoff <johann.haarhoff@gmail.com>
#
# This program is free software; you can redistribute it and/or modify
# it under the terms of Version 2 of the GNU General Public License as
# published by the Free Software Foundation.
# 
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.
#
# You should have received a copy of the GNU General Public License
# along with this program; if not, write to the Free Software
# Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
#
###############################################################################
#
# Originally written:
#    2008      Johann Haarhoff, <johann.haarhoff@gmail.com>
# Modifications:
#
###############################################################################

import sys, string

from xml.sax import saxutils, handler, make_parser

#
#  define the errors we use
#

class Error(Exception):
    """
    Just Extending Exception
    """
    pass

class OutOfOrderError(Error):
    """
    Thrown when you close a xml tag before
    all its children are closed
    """
    pass

class TagNotOpenError(Error):
    """
    Thrown when trying to close a tag that is
    not open
    """

class BasicXMLWriter(handler.ContentHandler):
    """
    This just inherits from handler.ContentHandler and
    allows basic output with indents etc.
    """

    def __init__(self, out = sys.stdout,indentstr = '\t'):
        handler.ContentHandler.__init__(self)
        self._out = out
	self._indentlevel = 0
	self._printnl = 1
	self._indentstr = indentstr

    # ContentHandler methods

    def startDocument(self):
        self._out.write('<?xml version="1.0" encoding="iso-8859-1"?>\n')

    def startElement(self, name, attrs):
        self._out.write('\n'+self._indentstr*self._indentlevel + '<' + name)
        for (name, value) in attrs.items():
            self._out.write(' %s="%s"' % (name, saxutils.escape(value)))
        self._out.write('>')
	self._indentlevel = self._indentlevel + 1

    def endElement(self, name):
	self._indentlevel = self._indentlevel - 1

	if self._printnl:
            self._out.write('\n' + self._indentstr*self._indentlevel + '</%s>' % name)
	else:
            self._out.write('</%s>' % name)

	self._printnl=1

    def characters(self, content):
        self._out.write(saxutils.escape(content))
	self._printnl = 0;

    def ignorableWhitespace(self, content):
        self._out.write(content)

    def processingInstruction(self, target, data):
        self._out.write('<?%s %s?>' % (target, data))


class BetterXMLWriter(BasicXMLWriter):
    """
    This makes life a bit easier by making sure the xml
    is relatively intact, it checks for closing tags
    out of order, and closing tags that have not been
    opened
    It also overrides endDocument to close the remaining
    open tags in the correct order.
    """

    def __init__(self, out = sys.stdout,indentstr = '\t'):
        BasicXMLWriter.__init__(self,out,indentstr)
	self._openElements = []

    def openElement(self,name,attrs={}):
	self.startElement(name,attrs)
	self._openElements.append(name)

    def closeLast(self):
	"""
	Closes the last open tag
	"""
	self.endElement(self._openElements.pop())

    def closeElement(self,name):
	if not (name in self._openElements):
	    raise TagNotOpenError()
	    #print 'TagNotOpenError()'
	elif not (name == self._openElements[len(self._openElements)-1]):
	    raise OutOfOrderError()
	    #print 'OutOfOrderError()'
	else:
	    self.endElement(name)
	    self._openElements.pop()

    def addData(self,content):
	"""
        this is truly just here because i think addData()
	is more descriptive than characters
	"""
	self.characters(content)

    def addCData(self,content):
	"""
	allows us to add CDATA sections
	"""
	self._out.write('<![CDATA[')
        self._out.write(content)
	self._out.write(']]>')


    def endDocument(self):
	for i in range(0,len(self._openElements)):
	    self.endElement(self._openElements.pop())


###############################################################################
# Copyright (C) 2008 Johann Haarhoff <johann.haarhoff@gmail.com>
#
# This program is free software; you can redistribute it and/or modify
# it under the terms of Version 2 of the GNU General Public License as
# published by the Free Software Foundation.
# 
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.
#
# You should have received a copy of the GNU General Public License
# along with this program; if not, write to the Free Software
# Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
#
###############################################################################
#
# Originally written:
#    2008      Johann Haarhoff, <johann.haarhoff@gmail.com>
# Modifications:
#
###############################################################################

#my modules

class Icon():
    def __init__(self,href=''):
	self._href = href

    def getIcon(self):
	return self._href

    def setIcon(self,href):
	self._href = href

    def toKML(self,out,indentstr = '\t'):
	kmlwriter = BetterXMLWriter(out,indentstr)
	kmlwriter.openElement("Icon")
	kmlwriter.openElement("href")
	kmlwriter.addData(str(self._href))
	kmlwriter.closeLast()
	kmlwriter.closeLast()

class hotspot():

    def __init__(self,x=0,y=0,xunits="pixels",yunits="pixels"):
	self._x = x
	self._y = y
	self._xunits = xunits
	self._yunits = yunits

    def getX(self):
	return self._x

    def setX(self,x):
	self._x = x

    def getY(self):
	return self._y

    def setY(self,y):
	self._y = y

    def getXunits(self):
	return self._xunits

    def setXunits(self,x):
	self._xunits = xunits

    def getYunits(self):
	return self._yunits

    def setYunits(self,y):
	self._yunits = yunits

    def toKML(self,out,indentstr = '\t'):
	kmlwriter = BetterXMLWriter(out,indentstr)
	kmlwriter.openElement("hotspot",{"x":str(self._x),"y":str(self._y),"xunits":str(self._xunits),"yunits":str(self._yunits)})

class ColorStyle():

    def __init__(self,color="",colorMode=""):
	self._color = color
	self._colorMode = colorMode

    def getColor(self):
	return self._color

    def setColor(self,color):
	self._color = color

    def getColorMode(self):
	return self._colorMode

    def setColorMode(self,colorMode):
	self._colorMode = colorMode

class IconStyle(ColorStyle):

    def __init__(self,color="",colorMode="",scale="",heading="",theIcon=Icon(),thehotspot=hotspot()):
	self._color = color
	self._colorMode = colorMode
	self._scale = scale
	self._heading = heading
	self._Icon = theIcon
	self._hotspot = thehotspot

    def getScale(self):
	return self._scale

    def setScale(self,scale):
	self._scale = scale

    def getIcon(self):
	return self._Icon

    def setIcon(self,theIcon):
	self._Icon = theIcon

    def getHeading(self):
	return self._heading

    def setHeading(self,heading):
	self._Icon = theIcon

    def getHotspot(self):
	return self._hotspot

    def setHotspot(self,thehotspot):
	self._hotspot = thehotspot

    def toKML(self,out,indentstr = '\t'):
	kmlwriter = BetterXMLWriter(out,indentstr)
	kmlwriter.openElement("IconStyle")
	kmlwriter.openElement("color")
	kmlwriter.addData(str(self._color))
	kmlwriter.closeLast()
	kmlwriter.openElement("colorMode")
	kmlwriter.addData(str(self._colorMode))
	kmlwriter.closeLast()
	kmlwriter.openElement("scale")
	kmlwriter.addData(str(self._scale))
	kmlwriter.closeLast()
	kmlwriter.openElement("heading")
	kmlwriter.addData(str(self._heading))
	kmlwriter.closeLast()
	self._Icon.toKML(out,indentstr)
	self._hotspot.toKML(out,indentstr)
	kmlwriter.closeLast()

class LabelStyle(ColorStyle):

    def __init__(self,color="",colorMode="",scale=""):
	self._color = color
	self._colorMode = colorMode
	self._scale = scale

    def getScale(self):
	return self._scale

    def setScale(self,scale):
	self._scale = scale

    def toKML(self,out,indentstr = '\t'):
	kmlwriter = BetterXMLWriter(out,indentstr)
	kmlwriter.openElement("LabelStyle")
	kmlwriter.openElement("color")
	kmlwriter.addData(str(self._color))
	kmlwriter.closeLast()
	kmlwriter.openElement("colorMode")
	kmlwriter.addData(str(self._colorMode))
	kmlwriter.closeLast()
	kmlwriter.openElement("scale")
	kmlwriter.addData(str(self._scale))
	kmlwriter.closeLast()
	kmlwriter.closeLast()

class LineStyle(ColorStyle):

    def __init__(self,color="",colorMode="",width=""):
	self._color = color
	self._colorMode = colorMode
	self._width = width

    def getWidth(self):
	return self._width

    def setWidth(self,width):
	self._width = width

    def toKML(self,out,indentstr = '\t'):
	kmlwriter = BetterXMLWriter(out,indentstr)
	kmlwriter.openElement("LineStyle")
	kmlwriter.openElement("color")
	kmlwriter.addData(str(self._color))
	kmlwriter.closeLast()
	kmlwriter.openElement("colorMode")
	kmlwriter.addData(str(self._colorMode))
	kmlwriter.closeLast()
	kmlwriter.openElement("width")
	kmlwriter.addData(str(self._width))
	kmlwriter.closeLast()
	kmlwriter.closeLast()

class PolyStyle(ColorStyle):

    def __init__(self,color="",colorMode="",fill="",outline=""):
	self._color = color
	self._colorMode = colorMode
	self._fill = fill
	self._outline = outline

    def getFill(self):
	return self._fill

    def setFill(self,fill):
	self._fill = fill

    def getOutline(self):
	return self._outline

    def setOutline(self,outline):
	self._outline = outline

    def toKML(self,out,indentstr = '\t'):
	kmlwriter = BetterXMLWriter(out,indentstr)
	kmlwriter.openElement("PolyStyle")
	kmlwriter.openElement("color")
	kmlwriter.addData(str(self._color))
	kmlwriter.closeLast()
	kmlwriter.openElement("colorMode")
	kmlwriter.addData(str(self._colorMode))
	kmlwriter.closeLast()
	kmlwriter.openElement("fill")
	kmlwriter.addData(str(self._fill))
	kmlwriter.closeLast()
	kmlwriter.openElement("outline")
	kmlwriter.addData(str(self._outline))
	kmlwriter.closeLast()
	kmlwriter.closeLast()
###############################################################################
# Copyright (C) 2008 Johann Haarhoff <johann.haarhoff@gmail.com>
#
# This program is free software; you can redistribute it and/or modify
# it under the terms of Version 2 of the GNU General Public License as
# published by the Free Software Foundation.
# 
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.
#
# You should have received a copy of the GNU General Public License
# along with this program; if not, write to the Free Software
# Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
#
###############################################################################
#
# Originally written:
#    2008      Johann Haarhoff, <johann.haarhoff@gmail.com>
# Modifications:
#
###############################################################################

#systemwide modules
import os
import glob
import math
import shapelibc
import dbflibc
import sys
import getopt
import readline

#my modules


###############################################################################
# utility functions 
###############################################################################

def sanitizeIntFromKeyboard(s,range_start=0,range_end=0):
    """
    input sanitazation, makes sure that whatever is passed in s is an int in the
    range given
    """
    try:
	x = int(s)
    except ValueError:
	err = 1
	return err,0

    if (x >= range_start) and (x <= range_end):
	err = 0
        return err,x
    else:
	err = 1
	return err,x

def sanitizeFloatFromKeyboard(s,range_start=0,range_end=0):
    """
    input sanitazation, makes sure that whatever is passed in s is a float in the
    range given
    """
    try:
	x = float(s)
    except ValueError:
	err = 1
	return err,0

    if (x >= range_start) and (x <= range_end):
	err = 0
        return err,x
    else:
	err = 1
	return err,x

def sanitizeStrFromKeyboard(s):
    return str(s)

###############################################################################
# program functions 
###############################################################################

def printBanner():
    print "This is shape2ge 0.1 (c) Johann Haarhoff"
    print ""
    return

def initGlobalsFromFile(filename):
    """
    At this stage we are defaulting the filename to sys.argv[1],
    but this should really be done with getopt
    """
    shpfile = shapelibc.open(str(sys.argv[1]),"rb")
    minbounds = shapelibc.ShapeFile_info(shpfile)[2]
    maxbounds = shapelibc.ShapeFile_info(shpfile)[2]
    dbffile = dbflibc.open(str(sys.argv[1]),"rb")
    numrecords = dbflibc.DBFFile_record_count(dbffile)
    numfields = dbflibc.DBFFile_field_count(dbffile)
    return

def usage():
    printBanner()
    print "This is how to use me"


def nodbf_handler():
    global numrecords
    numrecords = shapelibc.ShapeFile_info(shpfile)[0]

###############################################################################
# main 
###############################################################################

# globals
verbose = False
inputfile = ""
outputfile = ""

#parse our commandline options
try:
    opts, args = getopt.getopt(sys.argv[1:], "ho:v", ["help", "output="])
except getopt.GetoptError, err:
    print str(err) # will print something like "option -a not recognized"
    usage()
    sys.exit(2)
output = None
verbose = False
for o, a in opts:
    if o == "-v":
	verbose = True
    elif o in ("-h", "--help"):
	usage()
	sys.exit()
    elif o in ("-o", "--output"):
	outputfile = a
    else:
	assert False, "unhandled option"


#if no outputfile was specced, give it a name
if outputfile == "":
    outputfile = "shape2ge-conf.xml"


#try and figure out what was passed to us
#if we were passed something with a .shp extension, try and find a dbf
#if not, try and find a .shp and a dbf
inputfile = args[0]

#if the dude added the .shp we strip it
if inputfile[-4:0].upper() == ".SHP":
    inputfile = inputfile[:len(inputfile)-4]

#see if we can open it
try:
    shpfile = shapelibc.open(inputfile,"rb")
except IOError,err:
    #if we cannot open the shpfile we should die
    print str(err)
    sys.exit(2)

#we got this far, now try the dbf

nodbf=False
try:
    dbffile = dbflibc.open(inputfile,"rb")
except IOError,err:
    print """You did not supply a dbf file, all objects of the same type
    will be considered equal."""
    nodbf = True

# find out how many records we have
# if there is a dbf file we will consider it authorative
if nodbf == True:
    #do the whole thing for no dbf
    nodbf_handler()
    sys.exit(0)

numrecords = dbflibc.DBFFile_record_count(dbffile)
numfields = dbflibc.DBFFile_field_count(dbffile)

print "I found " + str(numfields) + " fields in the dbf file."
print "These are the field descriptions as they appear in"
print "the file."
print ""

for i in range(0,numfields):
    print str(i) + ": " + str(dbflibc.DBFFile_field_info(dbffile,i)[1])

print ""
print "Could you tell me which field refers to the type of"
print "feature you would like to split the data by. You will"
print "be able to assign different styles to features split "
print "in this step"

feattype = sanitizeIntFromKeyboard(raw_input("Which field refers to the feature type: "),0,30000)[1]

features = []

for i in range(0,numrecords):
    features.append(dbflibc.DBFFile_read_attribute(dbffile,i,feattype))

uniqfeatures = set(features)
uniqfeatures = sorted(uniqfeatures)

print "I found the following unique feature types in the file:"
print ""
for i,feat in enumerate(uniqfeatures):
    print str(i) + ": " + str(feat)

print "we will now proceed to create styles for these feature types"

stylelist = {}
for i,feat in enumerate(uniqfeatures):
    feat = str(feat).replace(" ","_")
    count = 0
    for j in range(0,numrecords):
	currentfeat = dbflibc.DBFFile_read_attribute(dbffile,j,feattype)
	if currentfeat == feat:
	    count = j
	    break

    shpobj = shapelibc.ShapeFile_read_object(shpfile,count)
    shptype = shapelibc.SHPObject_type_get(shpobj)

    if shptype in [shapelibc.SHPT_ARCZ,shapelibc.SHPT_ARC]:
	#make linestyle
	stylelist[feat] = []
	print str(feat)+" is an ARC, we are making a LineStyle..."
	color = raw_input("Give me the color quadlet (e.g. FF0000FF): ")
	colorMode = raw_input("Give me the color mode (e.g. normal): ")
	width = sanitizeFloatFromKeyboard(raw_input("Give me the line width (0.0-4.0): "),0,4)[1]
	stylelist[feat].append(LineStyle(color,colorMode,width))
    elif shptype in [shapelibc.SHPT_POLYGONZ,shapelibc.SHPT_POLYGON]:
	#make linestyle
	stylelist[feat] = []
	print str(feat)+""" is a POLY, we are making a LineStyle & PolyStyle...
	LineStyle first..."""
	color = raw_input("Give me the color quadlet (e.g. FF0000FF): ")
	colorMode = raw_input("Give me the color mode (e.g. normal): ")
	width = sanitizeFloatFromKeyboard(raw_input("Give me the line width (0.0-4.0): "),0,4)[1]
	stylelist[feat].append(LineStyle(color,colorMode,width))
	#make polystyle
	print "Now the PolyStyle..."
	color = raw_input("Give me the color quadlet (e.g. FF0000FF): ")
	colorMode = raw_input("Give me the color mode (e.g. normal): ")
	fill = sanitizeIntFromKeyboard(raw_input("Do you want the poly filled? (0:No 1:Yes): "),0,1)[1]
	outline = sanitizeIntFromKeyboard(raw_input("Do you want the poly outlined? (0:No 1:Yes): "),0,1)[1]
	stylelist[feat].append(PolyStyle(color,colorMode,fill,outline))
    elif shptype in [shapelibc.SHPT_POINTZ,shapelibc.SHPT_POINT]:
	#make iconstyle
	pass
    
kmlfile = open(outputfile,"w")
kmlwriter = BetterXMLWriter(kmlfile,"    ")
kmlwriter.startDocument()
kmlwriter.openElement("shp2kml")
kmlwriter.openElement("styles")

for s in stylelist.iteritems():
    kmlwriter.openElement("Style",{"id":s[0]})
    for j in s[1]:
	j.toKML(kmlfile)
    kmlwriter.closeLast()

kmlwriter.closeLast()
kmlwriter.openElement("feattype")
kmlwriter.openElement("feat_id")
kmlwriter.addData(str(feattype))
kmlwriter.closeLast()
kmlwriter.openElement("feat_name")
kmlwriter.addData("unknown")
kmlwriter.closeLast()
kmlwriter.closeLast()


x_scale = sanitizeFloatFromKeyboard(raw_input("What is the lattitude (x) Scale? : "),0,99999)[1]
y_scale = sanitizeFloatFromKeyboard(raw_input("What is the longitude (y) Scale? : "),0,99999)[1]

kmlwriter.openElement("scale")
kmlwriter.openElement("x_scale")
kmlwriter.addData(str(x_scale))
kmlwriter.closeLast()
kmlwriter.openElement("y_scale")
kmlwriter.addData(str(y_scale))
kmlwriter.closeLast()
kmlwriter.closeLast()

x_offset = sanitizeFloatFromKeyboard(raw_input("What is the lattitude (x) offset? : "),-99999,99999)[1]
y_offset = sanitizeFloatFromKeyboard(raw_input("What is the longitude (y) offset? : "),-99999,99999)[1]

kmlwriter.openElement("offset")
kmlwriter.openElement("x_offset")
kmlwriter.addData(str(x_offset))
kmlwriter.closeLast()
kmlwriter.openElement("y_offset")
kmlwriter.addData(str(y_offset))
kmlwriter.closeLast()
kmlwriter.closeLast()

kmlwriter.endDocument()

