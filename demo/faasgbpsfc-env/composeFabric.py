#!/usr/bin/python
import argparse
import requests,json
from requests.auth import HTTPBasicAuth
from subprocess import call
from infrastructure_config import *
import time
import sys
import os

DEFAULT_PORT='8181'

USERNAME='admin'
PASSWORD='admin'

OPER_OVSDB_TOPO='/restconf/operational/network-topology:network-topology/topology/ovsdb:1'

def get(host, port, uri):
    url = 'http://' + host + ":" + port + uri
    #print url
    r = requests.get(url, auth=HTTPBasicAuth(USERNAME, PASSWORD))
    jsondata=json.loads(r.text)
    return jsondata

def post(host, port, uri, data, debug=False):
    '''Perform a POST rest operation, using the URL and data provided'''

    url='http://'+host+":"+port+uri
    headers = {'Content-type': 'application/yang.data+json',
               'Accept': 'application/yang.data+json'}
    if debug == True:
        print "POST %s" % url
        print json.dumps(data, indent=4, sort_keys=True)
    r = requests.post(url, data=json.dumps(data), headers=headers, auth=HTTPBasicAuth(USERNAME, PASSWORD))
    if debug == True:
        print r.text
    r.raise_for_status()

    jsondata=json.loads(r.text)
    return jsondata
# Main Program

NODE_ID_OVSDB = ''
DEVICE_REF_PATTERN = "/network-topology:network-topology/network-topology:topology[network-topology:topology-id='ovsdb:1']/network-topology:node[network-topology:node-id='%s']"

URI_COMPOSE_FABRIC = "/restconf/operations/fabric:compose-fabric"
URI_ADD_FABRIC_LINK = "/restconf/operations/fabric-resources:add-fabric-link"

def compose_fabric(fabric, behavior):
    devNodes = list()
    
    for switch in switches:
        if switch["fabric"] == fabric:
            devNodes.append({"device-ref" : DEVICE_REF_PATTERN % switch['nodeid'], "vtep-ip":switch['vtep']})

    request_data = {
      "input" : {
           "name": "first fabric",
           "type":"VXLAN",
           "options":{
               "traffic-behavior":behavior
           },
           "device-nodes" : devNodes
       }
    }

    response_data = post(controller, DEFAULT_PORT, URI_COMPOSE_FABRIC, request_data, True)
    fabricid = response_data['output']['fabric-id']

    for switch in switches:
        if switch["fabric"] == fabric:
            switch['fabricid'] = fabricid

def createFabricLink(leftsw, rightsw):

    request_data = {
      "input" : {
           "source-fabric": leftsw['fabricid'],
           "source-fabric-port": leftsw['nodeid'] + '-' + "p-%s-to-%s" % (leftsw['name'], rightsw['name']),
           "dest-fabric":rightsw['fabricid'],
           "dest-fabric-port": rightsw['nodeid'] + '-' + "p-%s-to-%s" % (rightsw['name'], leftsw['name'])
       }
    }
    post(controller, DEFAULT_PORT, URI_ADD_FABRIC_LINK, request_data, True)

if __name__ == "__main__":


    behavior = "policy-driven"
    if len(sys.argv) > 1:
        if sys.argv[1] == "normal":
            behavior = "normal"
       

    controller=os.environ.get('ODL')
    if controller == None:
        sys.exit("No controller set.")

    fabrics = set()
    for switch in switches:
        if switch['fabric'] is not None:
            fabrics.add(switch['fabric'])

    print "get ovsdb node-id"
    time.sleep(3)
    dict_sw_node = {}
    ovsdb_topo = get(controller, DEFAULT_PORT,OPER_OVSDB_TOPO)["topology"]
    for topo_item in ovsdb_topo:
        if topo_item["node"] is not None:
            for ovsdb_node in topo_item["node"]:
                if ovsdb_node.has_key("ovsdb:bridge-name"):
                    switchname = ovsdb_node["ovsdb:bridge-name"]
                    for switch in switches:
                        if switchname == switch["name"]:
                            switch["nodeid"] = ovsdb_node["node-id"]
                            dict_sw_node[switchname] = switch['nodeid']

    print "compose fabric"
    for fabric in fabrics:
        if fabric is not None:
            compose_fabric(fabric, behavior)

    time.sleep(3)
    print "setup inter-connect for fabrics"
    for box in getAllBoxes():
        sw_in_box = getSwitchesByBox(box)
        if len(sw_in_box) > 1:
            for leftnode in sw_in_box:
                if not leftnode.has_key('fabricid'):
                    continue
                for rightnode in sw_in_box:
                    if not rightnode.has_key('fabricid'):
                        continue
                    if leftnode['fabricid'] != rightnode['fabricid']:
                        createFabricLink(leftnode, rightnode)

    print "setup outer-connect for fabrics"

