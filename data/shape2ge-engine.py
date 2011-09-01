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

from math import sqrt,acos,pi
from random import uniform
import decimal
from decimal import Decimal

def dot(vec1,vec2):
    return sum(x*y for x,y in zip(vec1,vec2))

def cross(vec1,vec2):
    """
    2d vectors only
    this tells us whether the direction vector
    is in or out of the 2d plane
    """
    #for 2d stuff, the first two terms dissapear, leaving us
    #only with the "z" term
    #return [(vec1[1]*0 - 0*vec2[1]), (0*vec2[0] - vec1[0]*0), (vec1[0]*vec2[1]-vec1[1]*vec2[0])]
    return (vec1[0]*vec2[1]-vec1[1]*vec2[0])

def magnitude(vec1):
    """
    2d vectors only
    """
    return sqrt(vec1[0]**2 + vec1[1]**2)

def unit_vec(vec1):
    return [vec1[0]/magnitude(vec1),vec1[1]/magnitude(vec1)]

def winding_number(poly):
    """
    poly is a [[x1,y1],[x2,y2]...,[x1,y1]]
    """
    #compute extents
    xmin = poly[0][0];
    ymin = poly[0][1];
    xmax = poly[0][0];
    ymax = poly[0][1];
    for i,j in poly:
	if i < xmin:
	    xmin = i;
	elif i > xmax:
	    xmax = i;

	if j < ymin:
	    ymin = j;
	elif j > ymax:
	    ymax = j;

    p = [0,0]
    angle = 0

    while Decimal(str(angle/(2*pi))).quantize(Decimal('1.'), rounding=decimal.ROUND_HALF_UP) == Decimal(0):
	# now we must try and find a point inside the polygon,
	# we do this by shooting a random point within the
	# extents
	p[0] = uniform(xmin,xmax)
	p[1] = uniform(ymin,ymax)
	angle = 0
	for i in range(0,len(poly) - 1):
	    vec1 = [poly[i][0] - p[0],poly[i][1] - p[1]]
	    vec2 = [poly[i+1][0] - p[0],poly[i+1][1] - p[1]]
	    dir = cross(vec1,vec2)
	    dp = dot(unit_vec(vec1),unit_vec(vec2))
	    # this next if is to handle precision errors
	    # since acos barfs on anything just slightly
	    # out of range
	    if dp > 1:
		dp = 1
	    elif dp < -1:
		dp = -1
	    newangle = acos(dp)
	    if dir > 0:
		newangle = -newangle
	    angle = angle + newangle
	    
    return Decimal(str(angle/(2*pi))).quantize(Decimal('1.'), rounding=decimal.ROUND_HALF_UP)
    
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

#global modules
import shapelibc
import dbflibc
import sys

#my modules

def castSpecific(shpobj):
    """
    if given a SHPObject, this will return a more
    specific version like SHPPointObject depending
    on the SHPType of the given object
    """
    if shpobj._SHPType == shapelibc.SHPT_POINT:
	obj = SHPPointObject()
	obj.createFromObject(shpobj)
	return obj
    elif shpobj._SHPType == shapelibc.SHPT_ARCZ:
	obj = SHPArcZObject()
	obj.createFromObject(shpobj)
	return obj
    elif shpobj._SHPType == shapelibc.SHPT_ARC:
	obj = SHPArcObject()
	obj.createFromObject(shpobj)
	return obj
    elif shpobj._SHPType == shapelibc.SHPT_POLYGONZ:
	obj = SHPPolygonZObject()
	obj.createFromObject(shpobj)
	return obj
    elif shpobj._SHPType == shapelibc.SHPT_POLYGON:
	obj = SHPPolygonObject()
	obj.createFromObject(shpobj)
	return obj
    

class WrongShapeObjectError(Exception):
    """
    Thrown when trying to instantiate say a
    SHPPointOPbject from file, and the file
    returns a different type
    """
    pass

class SHPObject():

    def __init__(self,SHPType = shapelibc.SHPT_NULL,SHPId = -1,Verts = [[]],Label="",Desc = ""):
	self._SHPType = SHPType
	self._SHPId = SHPId
	self._Verts = Verts
	self._Label = Label
	self._Desc = Desc

    def createFromFile(self,filestream,shapenum):
	"""
	The filestream should already be opened
	with shapelibc.open() before calling this
	"""
	shp = shapelibc.ShapeFile_read_object(filestream,shapenum)
	SHPObject.__init__(self,shapelibc.SHPObject_type_get(shp),
			    shapelibc.SHPObject_id_get(shp),
			    shapelibc.SHPObject_vertices(shp))

    def makeDescriptionFromFile(self,filestream,shapenum):
	"""
	The filestream should already be opened
	with dbflibc.open() before calling this
	"""
	numfields = dbflibc.DBFFile_field_count(filestream) 
	for i in range(0,numfields):
	    field_name = str(dbflibc.DBFFile_field_info(filestream,i)[1]).upper()
	    field_data = str(dbflibc.DBFFile_read_attribute(filestream,shapenum,i)).lower()
	    self._Desc = self._Desc + "<b>" + field_name + ": </b>" + field_data + "<br>"

class SHPPointObject(SHPObject):

    def __init__(self,SHPId = -1,Verts = [[]],Label="",Desc=""):
	SHPObject.__init__(self,shapelibc.SHPT_POINT,SHPId,Verts,Label,Desc)

    def createFromFile(self,filestream,shapenum):
	SHPObject.createFromFile(self,filestream,shapenum)
	if self._SHPType != shapelibc.SHPT_POINT:
	    raise WrongShapeObjectError()

    def createFromObject(self,shpobject):
	if shpobject._SHPType != shapelibc.SHPT_POINT:
	    raise WrongShapeObjectError()
	SHPPointObject.__init__(self,shpobject._SHPId,shpobject._Verts,shpobject._Label,shpobject._Desc)

    def toKML(self,out,styleUrl="",indentstr = '\t'):
	kmlwriter = BetterXMLWriter(out,indentstr)
	kmlwriter.openElement("Placemark")
	kmlwriter.openElement("name")
	if self._Label == "":
	    kmlwriter.addData(str(self._SHPId))
	else:
	    kmlwriter.addData(str(self._Label))
	kmlwriter.closeLast()
	kmlwriter.openElement("styleUrl")
	kmlwriter.addData(str(styleUrl))
	kmlwriter.closeLast()
	kmlwriter.openElement("description")
	kmlwriter.addCData(self._Desc)
	kmlwriter.closeLast()
	kmlwriter.openElement("Point")
	kmlwriter.openElement("coordinates")
	for i,j in self._Verts:
	    kmlwriter.addData(str(i)+","+str(j)+",0 ")

	kmlwriter.endDocument()

class SHPArcZObject(SHPObject):

    def __init__(self,SHPId = -1,Verts = [[]],Label="",Desc=""):
	SHPObject.__init__(self,shapelibc.SHPT_ARCZ,SHPId,Verts,Label,Desc)

    def createFromFile(self,filestream,shapenum):
	SHPObject.createFromFile(self,filestream,shapenum)
	if self._SHPType != shapelibc.SHPT_ARCZ:
	    raise WrongShapeObjectError()

    def createFromObject(self,shpobject):
	if shpobject._SHPType != shapelibc.SHPT_ARCZ:
	    raise WrongShapeObjectError()
	SHPArcZObject.__init__(self,shpobject._SHPId,shpobject._Verts,shpobject._Label,shpobject._Desc)

    def toKML(self,out,styleUrl="",indentstr = '\t'):
	kmlwriter = BetterXMLWriter(out,indentstr)
	kmlwriter.openElement("Placemark")
	kmlwriter.openElement("name")
	if self._Label == "":
	    kmlwriter.addData(str(self._SHPId))
	else:
	    kmlwriter.addData(str(self._Label))
	kmlwriter.closeLast()
	kmlwriter.openElement("styleUrl")
	kmlwriter.addData(str(styleUrl))
	kmlwriter.closeLast()
	kmlwriter.openElement("description")
	kmlwriter.addCData(self._Desc)
	kmlwriter.closeLast()
	kmlwriter.openElement("LineString")
	kmlwriter.openElement("tessellate")
	kmlwriter.addData("1")
	kmlwriter.closeLast()
	kmlwriter.openElement("coordinates")
	#shapelibc does not populate _Verts properly, 
	#so we need to check for the Z coordinate
	#even if this is an ArcZ
	if len(self._Verts[0][0]) == 2: 
	    #we only have x and y
	    for i,j in self._Verts[0]:
		kmlwriter.addData(str(i)+","+str(j)+",0 ")
	elif len(self._Verts[0][0]) == 3:
	    #we have x, y and z
	    for i,j,k in self._Verts[0]:
		kmlwriter.addData(str(i)+","+str(j)+","+str(k)+" ")
	elif len(self._Verts[0][0]) == 4:
	    #we have x,y,z and m
	    #I don't know what to do with m at this stage
	    for i,j,k,l in self._Verts[0]:
		kmlwriter.addData(str(i)+","+str(j)+","+str(k)+" ")

	kmlwriter.endDocument()

class SHPArcObject(SHPArcZObject):

    def __init__(self,SHPId = -1,Verts = [[]],Label="",Desc=""):
	SHPObject.__init__(self,shapelibc.SHPT_ARC,SHPId,Verts,Label,Desc)

    def createFromFile(self,filestream,shapenum):
	SHPObject.createFromFile(self,filestream,shapenum)
	if self._SHPType != shapelibc.SHPT_ARC:
	    raise WrongShapeObjectError()

    def createFromObject(self,shpobject):
	if shpobject._SHPType != shapelibc.SHPT_ARC:
	    raise WrongShapeObjectError()
	SHPArcObject.__init__(self,shpobject._SHPId,shpobject._Verts,shpobject._Label,shpobject._Desc)

class SHPPolygonZObject(SHPObject):

    def __init__(self,SHPId = -1,Verts = [[]],Label="",Desc=""):
	SHPObject.__init__(self,shapelibc.SHPT_POLYGONZ,SHPId,Verts,Label,Desc)

    def createFromFile(self,filestream,shapenum):
	SHPObject.createFromFile(self,filestream,shapenum)
	if self._SHPType != shapelibc.SHPT_POLYGONZ:
	    raise WrongShapeObjectError()

    def createFromObject(self,shpobject):
	if shpobject._SHPType != shapelibc.SHPT_POLYGONZ:
	    raise WrongShapeObjectError()
	SHPPolygonZObject.__init__(self,shpobject._SHPId,shpobject._Verts,shpobject._Label,shpobject._Desc)

    def toKML(self,out,styleUrl="",indentstr = '\t'):
	kmlwriter = BetterXMLWriter(out,indentstr)
	kmlwriter.openElement("Placemark")
	kmlwriter.openElement("name")
	if self._Label == "":
	    kmlwriter.addData(str(self._SHPId))
	else:
	    kmlwriter.addData(str(self._Label))
	kmlwriter.closeLast()
	kmlwriter.openElement("styleUrl")
	kmlwriter.addData(str(styleUrl))
	kmlwriter.closeLast()
	kmlwriter.openElement("description")
	kmlwriter.addCData(self._Desc)
	kmlwriter.closeLast()
	kmlwriter.openElement("Polygon")
	kmlwriter.openElement("extrude")
	kmlwriter.addData("0")
	kmlwriter.closeLast()
	kmlwriter.openElement("tessellate")
	kmlwriter.addData("1")
	kmlwriter.closeLast()
	#polygons may have multiple parts
	#in the shapefile, a part is an outer boundary if the
	#poly is wound clockwise, and an inner boundary if it
	#is wound anticlockwise.
	#we use winding_number in vec.py to figure this out

	for part,coords in enumerate(self._Verts):
	    dir = winding_number(coords)   #winding_number is from vec.py
	    if dir > 0:
		kmlwriter.openElement("outerBoundaryIs")
	    elif dir < 0:
		kmlwriter.openElement("innerBoundaryIs")

	    kmlwriter.openElement("LinearRing")
	    kmlwriter.openElement("coordinates")
	    #shapelibc does not populate _Verts properly, 
	    #so we need to check for the Z coordinate
	    #even if this is a PolygonZ
	    if len(self._Verts[part][0]) == 2: 
		#we only have x and y
		for i,j in self._Verts[part]:
		    kmlwriter.addData(str(i)+","+str(j)+",0 ")
	    elif len(self._Verts[part][0]) == 3:
		#we have x, y and z
		for i,j,k in self._Verts[part]:
		    kmlwriter.addData(str(i)+","+str(j)+","+str(k)+" ")
	    elif len(self._Verts[part][0]) == 4:
		#we have x,y,z and m
		#I don't know what to do with m at this stage
		for i,j,k,l in self._Verts[part]:
		    kmlwriter.addData(str(i)+","+str(j)+","+str(k)+" ")

	    kmlwriter.closeLast() #coordinates
	    kmlwriter.closeLast() #LinearRing
	    kmlwriter.closeLast() #outer/innerBoudary


	kmlwriter.endDocument()

class SHPPolygonObject(SHPPolygonZObject):
    def __init__(self,SHPId = -1,Verts = [[]],Label="",Desc=""):
	SHPObject.__init__(self,shapelibc.SHPT_POLYGON,SHPId,Verts,Label,Desc)

    def createFromFile(self,filestream,shapenum):
	SHPObject.createFromFile(self,filestream,shapenum)
	if self._SHPType != shapelibc.SHPT_POLYGON:
	    raise WrongShapeObjectError()

    def createFromObject(self,shpobject):
	if shpobject._SHPType != shapelibc.SHPT_POLYGON:
	    raise WrongShapeObjectError()
	SHPPolygonObject.__init__(self,shpobject._SHPId,shpobject._Verts,shpobject._Label,shpobject._Desc)

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

###############################################################################
# imports
###############################################################################

#global modules
import os
import glob
import math
import shapelibc
import dbflibc
from xml.dom import minidom
import xml.dom
import sys
import getopt

#my modules

###############################################################################
# global vars
###############################################################################

# these are filled from the shapefile
numrecords = 0
numfields = 0
minbounds = 0
maxbounds = 0
shpfile = None	    # global shapefile object
dbffile	= None    # global dbffile object
configfile = ""
outputfile = ""

# these are filled by the user or config file
x_offset = 0   # shapefile x offset relative to earth coords
y_offset = 0   # shapefile y offset relative to earth coords
z_offset = 0
x_scale = 1.0    # shapefile x scale to get to proper earth coords
y_scale = 1.0    # shapefile y scale to get to proper earth coords
z_scale = 1.0
feattype = 0	    # field num for differentiating different styles
featname = ""
verbose = False	    #verbose output

# these are derived from other conditions
configfile = ""
noconfig = False
nodbf = False

stylelist = {}

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
    print "This is shape2ge 0.1 (c) Johann Haarhoff 2008"
    print ""
    return

def getText(nodelist):
    rc = ""
    for node in nodelist:
        if node.nodeType == node.TEXT_NODE:
            rc = rc + node.data
    return rc

def usage():
    printBanner()

def parseCommandLine():
    global configfile
    global outputfile
    global noconfig
    global nodbf
    global shpfile
    global dbffile
    global verbose
    try:
	opts, args = getopt.getopt(sys.argv[1:], "hc:o:v", ["help", "configfile=" ,"output=", "verbose"])
    except getopt.GetoptError, err:
        print str(err) # will print something like "option -a not recognized"
        usage()
        sys.exit(2)
    output = None
    verbose = False
    for o, a in opts:
        if o in ("-v", "--verbose"):
            verbose = True
        elif o in ("-h", "--help"):
            usage()
            sys.exit()
        elif o in ("-o", "--output"):
            outputfile = a
	elif o in ("-c", "--config"):
	    configfile = a
        else:
            assert False, "unhandled option"

    if configfile == "":
	noconfig = True;

    #try and figure out what was passed to us
    #if we were passed something with a .shp extension, try and find a dbf
    #if not, try and find a .shp and a dbf
    inputfile = args[0]

    #if the dude added the .shp we strip it
    if inputfile[-4:0].upper() == ".SHP":
	inputfile = inputfile[:len(inputfile)-4]

    #if no outputfile was specced, give it a name
    outputfile = inputfile + ".kml"

    #see if we can open it
    try:
	shpfile = shapelibc.open(inputfile,"rb")
    except IOError,err:
	#if we cannot open the shpfile we should die
	print str(err)
	sys.exit(2)

    #we got this far, now try the dbf

    try:
	dbffile = dbflibc.open(inputfile,"rb")
    except IOError,err:
	print """You did not supply a dbf file, all objects of the same type
	will be considered equal."""
	nodbf = True


def parseConfigFile(filename):
    global x_offset   # shapefile x offset relative to earth coords
    global y_offset   # shapefile y offset relative to earth coords
    global z_offset   # shapefile z offset relative to earth coords
    global x_scale    # shapefile x scale to get to proper earth coords
    global y_scale    # shapefile y scale to get to proper earth coords
    global z_scale    # shapefile z scale to get to proper earth coords
    global feattype	    # field num for differentiating different styles
    global featname	    # field num for differentiating different styles
    global stylelist

    #fill a dict with styles
    dom = minidom.parse(filename)
    styl = dom.getElementsByTagName("styles")
    for st in styl:
	for style in st.getElementsByTagName("Style"):
	    id = style.getAttribute("id")
	    stylelist[id] = []
	    lstyles = style.getElementsByTagName("LineStyle")
	    for l in lstyles:
		tmp = l.getElementsByTagName("color")
		color = getText(tmp[0].childNodes)
		tmp = l.getElementsByTagName("colorMode")
		colorMode = getText(tmp[0].childNodes)
		tmp = l.getElementsByTagName("width")
		width = getText(tmp[0].childNodes)
		stylelist[id].append(LineStyle(color,colorMode,width))

	    pstyles = style.getElementsByTagName("PolyStyle")
	    for l in pstyles:
		tmp = l.getElementsByTagName("color")
		color = getText(tmp[0].childNodes)
		tmp = l.getElementsByTagName("colorMode")
		colorMode = getText(tmp[0].childNodes)
		tmp = l.getElementsByTagName("fill")
		fill = getText(tmp[0].childNodes)
		tmp = l.getElementsByTagName("outline")
		outline = getText(tmp[0].childNodes)
		stylelist[id].append(PolyStyle(color,colorMode,fill,outline))

    #get the feat type and name
    feat = dom.getElementsByTagName("feattype")
    for f in feat:
	tmp = f.getElementsByTagName("feat_id")
	feattype = int(getText(tmp[0].childNodes))
	tmp = f.getElementsByTagName("feat_name")
	featname = getText(tmp[0].childNodes)

    #get the offset and scales
    offsets = dom.getElementsByTagName("offset")
    for offset in offsets:
	tmp = offset.getElementsByTagName("x_offset")
	x_offset = float(getText(tmp[0].childNodes))
	tmp = offset.getElementsByTagName("y_offset")
	y_offset = float(getText(tmp[0].childNodes))

    scales = dom.getElementsByTagName("scale")
    for scale in scales:
	tmp = scale.getElementsByTagName("x_scale")
	x_scale = float(getText(tmp[0].childNodes))
	tmp = scale.getElementsByTagName("y_scale")
	y_scale = float(getText(tmp[0].childNodes))

    return

def getGlobalsFromShapeFile():
    global minbounds
    global maxbounds
    global numrecords
    minbounds = shapelibc.ShapeFile_info(shpfile)[2]
    maxbounds = shapelibc.ShapeFile_info(shpfile)[2]
    if nodbf:
	numrecords = shapelibc.ShapeFile_info(file)[0] 
    else:
        numrecords = dbflibc.DBFFile_record_count(dbffile)
    

def initGlobals():
    """
    parse the commandline, and see if we can open the conf and shp files
    """
    parseCommandLine()
    if not noconfig:
        parseConfigFile(configfile)
    getGlobalsFromShapeFile()

    

###############################################################################
# main 
###############################################################################

#offset_x = -0.000162
#offset_y = -0.000045

initGlobals()

kmlfile = open(outputfile,"w")
kmlwriter = BetterXMLWriter(kmlfile,"    ")
kmlwriter.startDocument()
kmlwriter.openElement("kml")
kmlwriter.openElement("Document")

#outputting styles

for s in stylelist.iteritems():
    kmlwriter.openElement("Style",{"id":s[0]})
    for j in s[1]:
	j.toKML(kmlfile)
    kmlwriter.closeLast()

#create a list of specobs
s = []
features = []
for i in range(0,numrecords):
    shpobj = SHPObject()
    shpobj.createFromFile(shpfile,i)
    shpobj.makeDescriptionFromFile(dbffile,i)
    s = castSpecific(shpobj)
    s._Label = "Shape Nr. "+str(i)
    
    features.append(dbflibc.DBFFile_read_attribute(dbffile,i,feattype))

    #scale first then offset
    if len(s._Verts[0][0]) == 2: 
	for p_index,p in enumerate(s._Verts):
	    for n_index,n in enumerate(s._Verts[p_index]):
		s._Verts[p_index][n_index] = ((s._Verts[p_index][n_index][0]*x_scale)+x_offset,
						(s._Verts[p_index][n_index][1]*y_scale)+y_offset)
    elif len(s._Verts[0][0]) > 2: 
	for p_index,p in enumerate(s._Verts):
	    for n_index,n in enumerate(s._Verts[p_index]):
		s._Verts[p_index][n_index] = ((s._Verts[p_index][n_index][0]*x_scale)+x_offset,
						(s._Verts[p_index][n_index][1]*y_scale)+y_offset,
						(s._Verts[p_index][n_index][2]*z_scale)+z_offset)

    #output the KML
    s.toKML(kmlfile,styleUrl="#"+str(features[i]).replace(" ","_"),indentstr = "    ")

kmlwriter.endDocument()
    
