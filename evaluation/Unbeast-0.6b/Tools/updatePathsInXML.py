#!/usr/bin/python
# -*- coding: utf-8 -*-
#
# A script to update an XML file in order to:
# - Include a correct path to the DTD file
# - Include a correct path to some LTL-to-Buechi tool
import os
import sys
import resource
import subprocess
import signal
import xml.dom.minidom

# =====================================================
# Function for printing usage information for the tool
# =====================================================
def usage():
    print "updatePathsInXML.py - A tool for updating an Unbeast specificiation XML file in order to contain correct paths to the syntax validation DTD file and the LTL-to-Büchi tool."
    print "(C) 2010 by Ruediger Ehlers."
    print ""
    print "Usage:"
    print "  /path/to/updatePathsInXML.py <path/to/XMLFile> <path/to/ltl-to-buechitool/tool> <ltl-to-buechitool parameters>"
    print ""
    print "For details, please see the README file enclosed in the Unbeast distribution."
    sys.exit(1)

if len(sys.argv)<3:
    usage()
    sys.exit(1)

# =====================================================
# Try to read XML file
# =====================================================
filename = sys.argv[1]
try:
    xmlFile = xml.dom.minidom.parse(filename)
except IOError:
    print "Error: Failed to read the XML file '"+filename+"' - probably it does not exist or you do not have sufficient rights."
    sys.exit(1)
except xml.parsers.expat.ExpatError:
    print "Error: Could not parse the XML file '"+filename+"' - probably it is not a valid XML file?"

# =====================================================
# Find the DTD file on disk
# =====================================================
if (sys.argv[0][0]<>os.sep):
    sys.argv[0] = os.sep+sys.argv[0]
absolutePath = os.getcwd()+sys.argv[0]
pos = absolutePath.rfind("updatePathsInXML.py")
absolutePath = absolutePath[0:pos]+"SynSpec.dtd"
if not os.path.exists(absolutePath):
    print "Error: The file '"+absolutePath+"' could not be found. However, the Unbeast distribution contains the file 'SynSpec.dtd' in the same directory as this script. As this file name has been produced by concatenating the script location and this string, this means that you have moved this script or deleted that file. Please undo this for proper operation."
    sys.exit(1)

# Now change the data
if xmlFile.doctype == None:
    print "Error: The file '"+filename+"' does not contain a DocType tag. Have a look at the README file of the Unbeast distribution for a valid input example"
    sys.exit(1)
xmlFile.doctype.systemId = absolutePath

# =====================================================
# Now try the LTL-to-Büchi converter
# =====================================================
cmdLine = " ".join(sys.argv[2:])+" '[] <> p' "
print "Trying to execute the LTL-to-Büchi converter: "+cmdLine

# Execute the converter
p = subprocess.Popen(cmdLine, shell=True,stdout=subprocess.PIPE,bufsize=1000000)
p.wait()

# Output & check result
outputOpened = False
outputClosed = False
for line in p.stdout:
    sys.stdout.write("O: "+line)
    if line.startswith("never {"):
        outputOpened = True
    if line.startswith("}"):
        outputClosed = True

if (not outputOpened) or (not outputClosed):
    print "Error: the LTL-to-Büchi converter specified does not yield a valid never claim. Note that both ltl2ba and spot need some additional parameters to run (which you need to supply):"
    print " - ltl2ba needs the parameter '-f'"
    print " - spot needs the paramter '-N' - you should also specify some simplification options, e.g., '-N -R3 -r7'"
    sys.exit(1)

# Alright? Then replace LTL-to-Büchi tool in XML file
replacedPath = False
for baseNode in xmlFile.childNodes:
    if baseNode.nodeType==baseNode.ELEMENT_NODE:
        if baseNode.nodeName<>"SynthesisProblem":
            print "Error: XML file seems invalid. Found a top-level '"+baseNode.nodeName+"' node."
            sys.exit(1)
        for basicNode in baseNode.childNodes:
            if basicNode.nodeType == basicNode.ELEMENT_NODE:
                if basicNode.nodeName=="PathToLTLCompiler":
                    # Search for a text node
                    if len(basicNode.childNodes)<>1:
                        print "Error: The PathToLTLCompiler XML node should only contain a single piece of text (no comments, etc.)"
                        sys.exit(1)
                    if basicNode.childNodes[0].nodeType!=basicNode.TEXT_NODE:
                        print "Error: The PathToLTLCompiler XML node should only contain a single piece of text (no comments, etc.)"
                        sys.exit(1)
                    replacedPath = True
                    basicNode.childNodes[0].nodeValue = " ".join(sys.argv[2:])

# =====================================================
# Write XML to file
# =====================================================
print "Writing back xml file..."

try:
    f = open(filename,'w')
except IOError:
    print "Error: Could not open '"+filename+"' for writing. Probably the file has the wrong permissions?"
    sys.exit(1)

try:
    f.write(xmlFile.toxml())
except IOError:
    print "Error: Writing to the XML file failed. Probably the disk is full?"

f.close()
sys.exit(0)

