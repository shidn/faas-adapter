#!/usr/bin/python
import argparse
import requests,json
from requests.auth import HTTPBasicAuth
from subprocess import call
import time
import sys
sys.path.append("..")
import os
from infrastructure_config import *

from restconf import *

OPER_OVSDB_TOPO='/restconf/operational/network-topology:network-topology/topology/ovsdb:1'
OPER_FABRIC_TOPO='/restconf/operational/network-topology:network-topology/topology/faas:fabrics'

OVS_BR_P = "/network-topology:network-topology/\
network-topology:topology[network-topology:topology-id='ovsdb:1']/\
network-topology:node[network-topology:node-id='%s']"

OVS_TP_P = "/network-topology:network-topology/\
network-topology:topology[network-topology:topology-id='ovsdb:1']/\
network-topology:node[network-topology:node-id='%s']/\
network-topology:termination-point[network-topology:tp-id='%s']"

EXTERNAL_FABRIC = 'ExternalNet'
EXTERNAL_LSW = 'lsw-external'


# Main definition - constants
 
# =======================
#     MENUS FUNCTIONS
# =======================
 
# Main menu

# =======================
#      MAIN PROGRAM
# =======================
 
# Main Program

URI_REG_ENDPOINT = "/restconf/operations/fabric-endpoint:register-endpoint"
URI_CREATE_LOGIC_PORT = "/restconf/operations/fabric-service:create-logical-port"
URI_CREATE_GATEWAY = '/restconf/operations/fabric-service:create-gateway'
URI_PORT_BINDING_DEV = '/restconf/operations/fabric-service:port-binding-logical-to-device'
URI_ADD_ROUTE = "/restconf/operations/fabric-service:add-static-route"
URI_ADD_FUNCTION = '/restconf/operations/fabric-service:add-port-function'


UUID_EPX_1 = 'b598c42c-e830-4458-b7bb-0c0b61982f42'
UUID_EPX_2 = 'fd8bd945-fe1d-4727-97bf-4572cc017303'

UUID_EXT_GW = '18221321-04b6-47e1-97c1-2c1e604058ab'

# mapping for subnet to logical switch
subnet2lsw = {"10.0.35.1": "vswitch-1", "10.0.36.1": "vswitch-2",
                "10.0.37.1": "vswitch-3"}

# global vlan for layer 2 inter connection
globalVlanTags = {"vswitch-1" : 100, "vswitch-2" : 101, "vswitch-3" : 102}

# temp var recordig port count of a logical switch
# key : (fabricid, switchname) , value : port count of logical switch
portidx_of_lsw = {}

# configuration all ip used for inter connection
inter_conn_gw_list = ["10.0.0.1", "10.0.0.2"]

# cache all inter connection gateway
# key : fabricid , value : { ip, mac}
inter_conn_gw_dict = {}

# cache all fabric inter connection port
# key : (fabric1, fabric2), value : (node, tp)
fabric_link_ports = {}

def create_logic_switch(fabricId, name, external=False):
    URI_CREATE_LOGIC_SWITCH = "/restconf/operations/fabric-service:create-logical-switch"

    request_data = {
        "input" : {
           "fabric-id": fabricId,
           "name":name,
           "external": external
         }
    }
    
    post(controller, DEFAULT_PORT, URI_CREATE_LOGIC_SWITCH, request_data, True)
    

def create_logic_router(fabricId, name):
    URI_CREATE_LOGIC_ROUTER = "/restconf/operations/fabric-service:create-logical-router"

    request_data = {
        "input" : {
           "fabric-id": fabricId,
           "name":name
         }
    }
   
    post(controller, DEFAULT_PORT, URI_CREATE_LOGIC_ROUTER, request_data, True)

def create_normal_logic_port(host):
    request_data = {
        "input" : {
           "fabric-id": host['fabricid'],
           "name":host['logical-port'],
           "logical-device":host['lsw']
       }
    }
   
    post(controller, DEFAULT_PORT, URI_CREATE_LOGIC_PORT, request_data, True)

def create_logic_port(fabricId, deviceName, portName):
    request_data =  {
        "input" : {
           "fabric-id": fabricId,
           "name":portName,
           "logical-device":deviceName
       }
    }
    response_data = post(controller, DEFAULT_PORT, URI_CREATE_LOGIC_PORT, request_data, True)
    return response_data['output']['tp-id']

def create_logic_port_withVlan(fabricId, deviceName, portName, vlan):
    request_data = {
        "input" : {
           "fabric-id": fabricId,
           "name":portName,
           "logical-device":deviceName,
           "attribute":{
               "port-layer" : {
                   "layer-2-info":{
                       "access-type":"vlan",
                       "access-segment":vlan
                   }
               }
           }
       }
    }
    response_data = post(controller, DEFAULT_PORT, URI_CREATE_LOGIC_PORT, request_data, True)
    return response_data['output']['tp-id']

def port_binding_dev(fabricId, vswitch, logicalport, deviceNodeId, physicalPort):
    request_data = {
      "input" : {
           "fabric-id": fabricId,
           "logical-device":vswitch,
           "logical-port":logicalport,
           "physical-port":OVS_TP_P % (deviceNodeId, physicalPort)
       }
    }
    post(controller, DEFAULT_PORT, URI_PORT_BINDING_DEV, request_data, True)

def create_gateway(fabricId, ipaddr, network, switchName):
    """ create gateway port , and return mac of gateway
    """

    request_data = {
      "input" : {
           "fabric-id": fabricId,
           "ip-address":ipaddr,
           "network":network,
           "logical-router":"vrouter-1",
           "logical-switch":switchName
       }
    }
    
    response_data = post(controller, DEFAULT_PORT, URI_CREATE_GATEWAY, request_data, True)
    return response_data['output']['port-layer']['layer-3-info']['mac']

def prepare_host_data():
    for host in hosts:
        if host['name'].startswith('hostx'):
            fabricid = EXTERNAL_FABRIC
            lsw = EXTERNAL_LSW
            host['lsw'] = lsw
            host['fabricid'] = fabricid

            portnum = 1
            if portidx_of_lsw.has_key((fabricid, lsw)):
                portidx_of_lsw[fabricid, lsw] += 1
                portnum = portidx_of_lsw[fabricid, lsw]
            else:
                portidx_of_lsw[fabricid, lsw] = 1

            lport = lsw + ' - p-' + str(portnum)
            host['logical-port'] = lport
            host['physical-port'] = 'vethl-' + str(host['name'])
        else:
            ip = host["ip"].split("/")[0]
            gw = get_default_gw(ip)
            host['gateway'] = gw
            lsw = subnet2lsw[gw]
            host['lsw'] = lsw
            fabricid = ''

            for switch in switches:
                if host['switch'] == switch['name']:
                    fabricid = switch['fabricid']
                    host['fabricid'] = fabricid
                    host['device-id'] = switch['nodeid']

            portnum = 1
            if portidx_of_lsw.has_key((fabricid, lsw)):
                portidx_of_lsw[(fabricid, lsw)] += 1
                portnum = portidx_of_lsw[(fabricid, lsw)]
            else:
                portidx_of_lsw[(fabricid, lsw)] = 1
            
            lport = lsw + ' - p-' + str(portnum)
            host['logical-port'] = lport
            host['physical-port'] = 'vethl-' + str(host['name'])


def register_normal_endpoint(host):
    request_data = {
        "input": {
            "fabric-id": host['fabricid'],
            "mac-address": host['mac'],
            "ip-address": host["ip"].split("/")[0],
            "gateway": host['gateway'],
            "logical-location": {
                "node-id": host['lsw'],
                "tp-id": host['logical-port']
            },
            "location": {
                "node-ref": OVS_BR_P % (host['device-id']),
                "tp-ref": OVS_TP_P % (host['device-id'], host['physical-port']),
                "access-type": "exclusive"
            }
        }
    }
   
    post(controller, DEFAULT_PORT, URI_REG_ENDPOINT, request_data, True)


def get_fabric_inter_connect_port():
    fabric_topo = get(controller, DEFAULT_PORT, OPER_FABRIC_TOPO)['topology']
    for topo_item in fabric_topo:
        if topo_item['link'] is not None:
            for f_link in topo_item['link']:
                src_fabric = f_link['source']['source-node']
                src_tp = f_link['source']['source-tp']
                stmp = src_tp.split(' - p-')
                dst_fabric = f_link['destination']['dest-node']
                dst_tp = f_link['destination']['dest-tp']
                fabric_link_ports[(src_fabric, dst_fabric)] = (stmp[0], 'p-' + stmp[1])

def reg_inter_conn_endpoint(fabricid, mac, ip, lportid, fabricid2):
    physicalNode = fabric_link_ports[(fabricid, fabricid2)][0]
    physicalTp = fabric_link_ports[(fabricid, fabricid2)][1]
    request_data = {
        "input": {
            "fabric-id": fabricid,
            "mac-address": mac,
            "ip-address": ip,
            #"gateway": host['gateway'],
            "logical-location": {
                "node-id": 'lsw-inter-con',
                "tp-id": lportid
            },
            "location": {
                "node-ref": OVS_BR_P % (physicalNode),
                "tp-ref": OVS_TP_P % (physicalNode, physicalTp),
                "access-type":"vlan",
                "access-segment":1000
            }
        }
    }
   
    post(controller, DEFAULT_PORT, URI_REG_ENDPOINT, request_data, True)

def rpc_register_endpoint_uri():
    return "/restconf/operations/fabric-endpoint:register-endpoint"

def rpc_reg_external_gw_ep_data(fabricid, epId, ipaddr, pDevice, pPort):
    return {
        "input" : {
           "fabric-id": fabricid,
           "endpoint-uuid":epId,
           "mac-address":floating_ip_gw_mac,
           "ip-address":ipaddr,
            "logical-location": {
                "node-id":"lsw-external",
                "tp-id":"lsw-external-p-1"
            },
           "location" : {
                "node-ref": OVS_BR_P % (dict_sw_node[pDevice]),
                "tp-ref": OVS_TP_P % (dict_sw_node[pDevice], pPort),
                "access-type":"exclusive"
           }
       }
    }


def add_routes(fabricId, tgtHosts, nexthop, interface):

    routes = list()
    
    for host in tgtHosts:
        if host["fabricid"] == fabricId:
            routes.append({"destination-prefix" : host["ip"].split("/")[0] + '/32', "next-hop" : nexthop, "outgoing-interface" : interface})

    request_data = {
      "input" : {
           "fabric-id" : fabricId,
           "node-id" : "vrouter-1",
           "route" : routes
        }
    }
   
    post(controller, DEFAULT_PORT, URI_ADD_ROUTE, request_data, True)

def rpc_add_route(fabricId, destIp, nexthop, interface):
    return {
      "input" : {
           "fabric-id" : fabricId,
           "node-id" : "vrouter-1",
           "route" : [
               { "destination-prefix" : destIp,
                 "next-hop" : nexthop,
                 "outgoing-interface" : interface
               }
           ]
        }
    }

def rpc_add_function_data(fabricId, extIp, interIp):
    return {
      "input" : {
           "fabric-id" : fabricId,
           "logical-device" : "vrouter-1",
           "logical-port" : "192.168.1.0",
           "port-function" : {
               "ip-mapping-entry" :[
                     { "external-ip" : extIp,
                       "internal-ip" : interIp
                     }
               ]
            }
        }
    }

def get_default_gw(ipaddr):
    arr = ipaddr.split(".")
    return "%s.%s.%s.%s" % (arr[0], arr[1], arr[2], "1")



def pause():
    print "press Enter key to continue..."
    raw_input()

if __name__ == "__main__":
    # Launch main menu


    # Some sensible defaults
    controller = os.environ.get('ODL')
    if controller == None:
        sys.exit("No controller set.")

    print "get ovsdb node-id"
    fabrics = set()
    dict_sw_node = {}
    ovsdb_topo = get(controller, DEFAULT_PORT, OPER_OVSDB_TOPO)["topology"]
    for topo_item in ovsdb_topo:
        if topo_item["node"] is not None:
            for ovsdb_node in topo_item["node"]:
                if "ovsdb:bridge-name" in ovsdb_node:
                    switchname = ovsdb_node["ovsdb:bridge-name"]
                    for switch in switches:
                        if switchname == switch["name"]:
                            switch["nodeid"] = ovsdb_node["node-id"]
                            dict_sw_node[switchname] = switch['nodeid']
                            if ovsdb_node.has_key("fabric-capable-device:attributes"):
                                switch["fabricid"] = ovsdb_node["fabric-capable-device:attributes"]["fabric-id"]
                                fabrics.add(switch["fabricid"])
                            else:
                                print "has not attribute fabric. switch = %s" % switchname

    get_fabric_inter_connect_port()

    print "Ready to create logical switch. Hit any key to continue."
    pause()
    for (subnet,lsw) in  subnet2lsw.items():
        for fabricid in fabrics:
            create_logic_switch(fabricid, lsw)

    print "Logical switch has been created."
    print ""

    print "Ready to create Logical Router. Hit any key to continue."
    pause() 
    for fabricid in fabrics:
        create_logic_router(fabricid, "vrouter-1")

    print "Logical Router has been created."
    print ""


    print "Ready to register Endpoints. Hit any key to continue."
    prepare_host_data()

    for host in hosts:
        if not host['name'].startswith('hostx'):
            # create logic port
            create_normal_logic_port(host)
            # register endpoint
            register_normal_endpoint(host)

    print "Endpoints have been created."
    print ""

    print "create gateway ..."
    pause()

    for (subnet,lsw) in  subnet2lsw.items():
        for fabricid in fabrics:
            create_gateway(fabricid, subnet, subnet + '/24', lsw)

    print "Gateway have been created."
    print ""

    #---------------------------------- layer2 connectivity -------------------------------------
    print "create layer2 forwarder..."
    pause()
    
    for fabricid1 in fabrics:
        for fabricid2 in fabrics:
            if fabric_link_ports.has_key((fabricid1, fabricid2)):
                physicalNode = fabric_link_ports[(fabricid1, fabricid2)][0]
                physicalTp = fabric_link_ports[(fabricid1, fabricid2)][1]

                for (lsw ,vlan) in globalVlanTags.items():
                    lportid = create_logic_port_withVlan(fabricid1, lsw, lsw + '-p-con-' + fabricid2, vlan)
                    port_binding_dev(fabricid1, lsw, lportid, physicalNode, physicalTp)

    print "layer2 forwarder have been created."
    print ""

    #----------------------------------- layer3 connectivity -------------------------------------
    print "create inter-connect switch..."
    pause()
    for fabricid in fabrics:
        create_logic_switch(fabricid, "lsw-inter-con")

    print "create inter-connect gateway..."
    pause()
    idx = 0
    for fabricid in fabrics:
        ip = inter_conn_gw_list[idx]
        mac = create_gateway(fabricid, inter_conn_gw_list[idx], "10.0.0.0/24", "lsw-inter-con")
        # save fabricid & mac info for endpoint registion
        inter_conn_gw_dict[fabricid] = {'ip':ip, 'mac':mac}
        idx += 1

    print "create inter-connect port..."
    pause()
    for fabricid in fabrics:
        create_logic_port(fabricid, "lsw-inter-con", "inter-con-p-1")

    print "register layer3 inter-connect endpoint..."
    pause()
    idx = 0
    for (k,v) in  inter_conn_gw_dict.items():
        for fabricid in fabrics:
            if fabricid != k:
                lport_id = create_logic_port(fabricid, "lsw-inter-con", "inter-con-p-%d" % idx)
                reg_inter_conn_endpoint(fabricid, v['mac'], v['ip'], lport_id, k)
                idx += 1
                
    print "create layer3 static route..."
    pause()
    
    for (k,v) in  inter_conn_gw_dict.items():
        for fabricid in fabrics:
            if fabricid != k:
                targetHosts = filter(lambda x: x['fabricid'] == fabricid, hosts)
                add_routes(fabricid, targetHosts, v['ip'], inter_conn_gw_dict[fabricid]['ip'])
       
    
    #----------------------------------- NAT -------------------------------------
    print "enable NAT Function..."

    pause()
    # create external logical switch
    create_logic_switch("fabric:2", "lsw-external", True)
    # create logical port
    create_logic_port("fabric:2", "lsw-external", "lsw-external-p-1")
    # register external gateway endpoint
    post(controller, DEFAULT_PORT, URI_REG_ENDPOINT, rpc_reg_external_gw_ep_data("fabric:2", UUID_EXT_GW, "192.168.1.1", "sw2", "p-s22-to-sw3"), True)
    # create outgoing logical port
    create_gateway("fabric:2", "192.168.1.0", "192.168.1.0/24", "lsw-external")
    # add default routing
    post(controller, DEFAULT_PORT, URI_ADD_ROUTE, rpc_add_route("fabric:2", "192.168.2.0/24", "192.168.1.1", "192.168.1.0"), True)
    # add NAT function
    post(controller, DEFAULT_PORT, URI_ADD_FUNCTION, rpc_add_function_data("fabric:2", "192.168.1.2", "10.0.35.8"), True)

