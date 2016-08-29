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
from infrastructure_config import *

def addController(sw, ip):
    call(['ovs-vsctl', 'set-controller', sw, 'tcp:%s:6653' % ip ])

def addManager(ip):
    cmd="ovs-vsctl set-manager tcp:%s:6640" % ip
    listcmd=cmd.split()
    print check_output(listcmd)

def addSwitch(name, dpid=None):
    call(['ovs-vsctl', 'add-br', name]) #Add bridge
    if dpid:
        if len(dpid) < 16: #DPID must be 16-bytes in later versions of OVS
            filler='0000000000000000'
            dpid=filler[:len(filler)-len(dpid)]+dpid
        elif len(dpid) > 16:
            print 'DPID: %s is too long' % dpid
            sys.exit(3)
        call(['ovs-vsctl','set','bridge', name,'other-config:datapath-id=%s'%dpid])

def addHost(net, switch, name, ip, mac):
    containerID=launchContainer()

def setOFVersion(sw, version='OpenFlow13,OpenFlow12,OpenFlow10'):
    call(['ovs-vsctl', 'set', 'bridge', sw, 'protocols={}'.format(version)])

def launchContainer(host,containerImage):
    containerID= check_output(['docker','run','-d','--net=none','--name=%s'%host['name'],'-h',host['name'],'-t', '-i','--privileged=True',containerImage,'/bin/bash']) #docker run -d --net=none --name={name} -h {name} -t -i {image} /bin/bash
    #print "created container:", containerID[:-1]
    return containerID[:-1] #Remove extraneous \n from output of above

def connectContainerToSwitch(sw,host,containerID,of_port):
    hostIP=host['ip']
    mac=host['mac']
    nw = ipaddr.IPv4Network(hostIP)
    broadcast = "{}".format(nw.broadcast)
    router = "{}".format(nw.network + 1)
    cmd=['/vagrant/ovswork.sh',sw,containerID,hostIP,broadcast,router,mac,of_port,host['name']]
    if host.has_key('vlan'):
        cmd.append(host['vlan'])
    call(cmd)

def doCmd(cmd):
    listcmd=cmd.split()
    print check_output(listcmd)

def launch(sw, hosts, contIP='127.0.0.1'):

    #addManager(contIP)
    dpid=sw['dpid']
    addSwitch(sw['name'],dpid)
    setOFVersion(sw['name'])
    addController(sw['name'], contIP)
    ports=0

    if sw.has_key('vtep'):
        os.system("ifconfig %s %s" % (sw['name'], sw['vtep']))
        os.system("ovs-vsctl br-set-external-id %s vtep-ip %s" % (sw['name'], sw['vtep']))

    for host in hosts:
        if host['switch'] == sw['name']:
            containerImage=defaultContainerImage #from Config
            if host.has_key('container_image'): #from Config
                containerImage=host['container_image']
            containerID=launchContainer(host,containerImage)
            ports+=1
            connectContainerToSwitch(sw['name'],host,containerID,str(ports))
            host['ofport'] = ports
            host['port-name']='vethl-'+host['name']
            print "Created container: %s with IP: %s. Connect using 'docker attach %s', disconnect with ctrl-p-q." % (host['name'],host['ip'],host['name'])


def setupInterConnect(switches):
    ofPortOnExt = 100
    for leftNode in switches:
        for rightNode in switches:
            if leftNode['name'] != rightNode['name']:
                leftPort = 'p-%s-to-%s' % (leftNode['name'], rightNode['name'])
                rightPort = 'p-%s-to-%s' % (rightNode['name'], leftNode['name'])
                #call(['ovs-vsctl','add-port',leftNode, 'p-%s-to-%s' % (leftNode,rightNode)])
                #call(['ovs-vsctl','set','interface', 'p-%s-to-%s' % (leftNode, rightNode), 'type=patch'])
                #call(['ovs-vsctl','set','interface', 'p-%s-to-%s' % (leftNode, rightNode), 'options:peer=p-%s-to-%s' % (rightNode, leftNode)])

                if leftNode['fabric'] is None:
                    call(['ovs-vsctl','add-port',leftNode['name'],leftPort,'--','set','interface',leftPort,'type=patch','options:peer=%s' % rightPort, 'ofport_request=%d' % ofPortOnExt])
                    cache_ofport_ext[rightNode['fabric']] = ofPortOnExt
                    ofPortOnExt += 1
                else:
                    call(['ovs-vsctl','add-port',leftNode['name'],leftPort,'--','set','interface',leftPort,'type=patch','options:peer=%s' % rightPort])

# learning mac from arp request for spine device
SPINE_ARP_LEARNING = "ovs-ofctl add-flow -OOpenFlow13 %s 'table=10, priority=1024, reg0=0x3, arp, arp_op=1 actions=learn(table=110,priority=1024,NXM_NX_TUN_ID[],NXM_OF_ETH_DST[]=NXM_OF_ETH_SRC[],load:NXM_OF_VLAN_TCI[]->NXM_OF_VLAN_TCI[],output:NXM_OF_IN_PORT[]),goto_table:20'"
# learning mac from normal packets for spine device
SPINE_NORMAL_LEARNING = "ovs-ofctl add-flow -OOpenFlow13 %s 'table=10, priority=1023, reg0=0x3, vlan_tci=0x1000/0x1000, actions=learn(table=110,priority=1024,NXM_NX_TUN_ID[],NXM_OF_ETH_DST[]=NXM_OF_ETH_SRC[],load:NXM_OF_VLAN_TCI[]->NXM_OF_VLAN_TCI[],output:NXM_OF_IN_PORT[]),strip_vlan,goto_table:20'"
# learning mac for leaf device
LEAF_LEARNING = "ovs-ofctl add-flow -OOpenFlow13 %s 'table=10, priority=1024, reg0=0x2, actions=learn(table=110,priority=1024,NXM_NX_TUN_ID[],NXM_OF_ETH_DST[]=NXM_OF_ETH_SRC[],load:NXM_NX_TUN_IPV4_SRC[]->NXM_NX_TUN_IPV4_DST[],output:NXM_OF_IN_PORT[]),goto_table:20'"


# static flow on external switch, for packets come from fabric,
# parameter : [SwitchName, floating_ip, HOSTX_IP, HOSTX_MAC, HOSTX_GW_MAC, HOSTX_of_port]
NAT_FROM_FABRIC_FLOW = "ovs-ofctl add-flow -OOpenFlow13 %s \
'table=0,ip, nw_src=%s, nw_dst=%s actions=set_field:%s->eth_dst, set_field:%s->eth_src, output:%d'"

# static flow on external switch, for packets going to fabric
# parameter : [SwitchName, HOSTX_IP, FLOATING_IP, out_ofport]
NAT_TO_FABRIC_FLOW = "ovs-ofctl add-flow -OOpenFlow13 %s \
'table=0,ip, nw_src=%s, nw_dst=%s actions=set_field:80:38:bc:a1:33:c7->eth_dst, set_field:%s->eth_src, output:%d'"

# arp responser for hostx
# parameter : [SwitchName, HOSTX_GW, HOSTX_GW_MAC, HOSTX_GW_MAC_HEX, HOSTX_GW_HEX]
NAT_ARP_RESPONDER = "ovs-ofctl add-flow -OOpenFlow13 %s \
'table=0, arp,arp_tpa=%s,arp_op=1 \
actions=move:NXM_OF_ETH_SRC[]->NXM_OF_ETH_DST[],\
set_field:%s->eth_src,load:0x2->NXM_OF_ARP_OP[],\
move:NXM_NX_ARP_SHA[]->NXM_NX_ARP_THA[],\
move:NXM_OF_ARP_SPA[]->NXM_OF_ARP_TPA[],\
load:%s->NXM_NX_ARP_SHA[],\
load:%s->NXM_OF_ARP_SPA[],IN_PORT'"

HOSTX_GW_MAC = '62:02:1a:00:00:01'
HOSTX_GW_MAC_HEX = '0x62021a000001'

def setupDefaultFlow(switch, numPerHost):
    if switch['fabric'] is not None:
        if numPerHost > 1:
            os.system(SPINE_ARP_LEARNING % switch['name'])
            os.system(SPINE_NORMAL_LEARNING % switch['name'])
        else:
            os.system(LEAF_LEARNING % switch['name'])

def setupFlowForExternlNet(localswitches):
    ext_switch = filter(lambda x : x['fabric'] is None, localswitches)[0]
    
    fab_switches = filter(lambda x : x['fabric'] is not None, localswitches)

    allhostx = filter(lambda x : x['switch'] == ext_switch['name'], hosts)

    ip2num = lambda x : sum([256**j*int(i) for j,i in enumerate(x.split('.')[::-1])])

    for fsw in fab_switches:
        fabric = fsw['fabric']
        tgtFloatingIps = filter(lambda x : x['fabric'] == fabric, floating_ip_pool)
        ofport_to_fabric = cache_ofport_ext[fabric]
        for floatingIp in tgtFloatingIps:
            print floatingIp
            for hostx in allhostx:
                ip_hostx = hostx["ip"].split("/")[0]
                # from fabric
                os.system(NAT_FROM_FABRIC_FLOW % (ext_switch['name'], floatingIp['ip'], ip_hostx, hostx['mac'], HOSTX_GW_MAC, hostx['ofport']))
                # to fabric
                os.system(NAT_TO_FABRIC_FLOW % (ext_switch['name'], ip_hostx, floatingIp['ip'], floating_ip_gw_mac, ofport_to_fabric))

    for hostx in allhostx:
        ip_hostx = hostx["ip"].split("/")[0]
        gw_hostx = get_default_gw(ip_hostx)
        # arp reply for hostx's gateway
        os.system(NAT_ARP_RESPONDER % (ext_switch['name'], gw_hostx, HOSTX_GW_MAC, HOSTX_GW_MAC_HEX, hex(ip2num(gw_hostx))))

def get_default_gw(ipaddr):
    arr = ipaddr.split(".")
    return "%s.%s.%s.%s" % (arr[0], arr[1], arr[2], "1")

# cache ofport on external switch
# key : fabric, value : ofport
cache_ofport_ext = {}

if __name__ == "__main__" :

    hostname = socket.gethostname()

    localswitches = filter(lambda x : x['host'] == hostname, switches)
    controller = os.environ.get('ODL')

    if len(localswitches) > 0:
        addManager(controller)

    for switch in localswitches:
        sw_name = switch['name']
        launch(switch,hosts,controller)

    if len(localswitches) > 1:
        setupInterConnect(localswitches)

    for localswitch in localswitches:
        setupDefaultFlow(localswitch, len(localswitches))

    if len(localswitches) > 1:
        setupFlowForExternlNet(localswitches)
