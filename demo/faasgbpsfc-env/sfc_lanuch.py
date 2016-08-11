#!/usr/bin/python

import socket
import os
import re
import time
import sys
import ipaddr
import commands
from subprocess import call
from subprocess import check_output
from sfc_config import *

def doCmd(cmd):
    listcmd=cmd.split()
    print check_output(listcmd)

if __name__ == "__main__" :

    hostname = socket.gethostname()
    for switch in switches:
        if hostname == switch['host']:

            controller=os.environ.get('ODL')
            sw_type = switch['type']
            sw_name = switch['name']
            if sw_type == 'sff':
                print "*****************************"
                print "Configuring %s as an SFF." % sw_name
                print "*****************************"
                doCmd('sudo ovs-vsctl set-manager tcp:%s:6640' % controller)
                print
            elif sw_type == 'sf':
                print "*****************************"
                print "Configuring %s as an SF." % sw_name
                print "*****************************"
                doCmd('sudo /vagrant/sf-config.sh ' + sw_name)
