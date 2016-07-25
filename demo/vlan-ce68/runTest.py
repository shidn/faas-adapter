#!/usr/bin/python
import argparse
import requests,json
from requests.auth import HTTPBasicAuth
from subprocess import call
import time
import sys
import os


DEFAULT_PORT='8181'

USERNAME='admin'
PASSWORD='admin'

def get(host, port, uri):
    url = 'http://' + host + ":" + port + uri
    #print url
    r = requests.get(url, auth=HTTPBasicAuth(USERNAME, PASSWORD))
    jsondata=json.loads(r.text)
    return jsondata

def put(host, port, uri, data, debug=False):
    '''Perform a PUT rest operation, using the URL and data provided'''

    url='http://'+host+":"+port+uri

    headers = {'Content-type': 'application/yang.data+json',
               'Accept': 'application/yang.data+json'}
    if debug == True:
        print "PUT %s" % url
        print json.dumps(data, indent=4, sort_keys=True)
    r = requests.put(url, data=json.dumps(data), headers=headers, auth=HTTPBasicAuth(USERNAME, PASSWORD))
    if debug == True:
        print r.text
    r.raise_for_status()

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
    

# Main definition - constants
 
# =======================
#     MENUS FUNCTIONS
# =======================
 
# Main menu

# =======================
#      MAIN PROGRAM
# =======================
 
# Main Program

UUID_EP1 = '75a4451e-eed0-4645-9194-64454bda2902'
UUID_EP2 = 'ad08c19c-32cc-4cee-b902-3f4919f51bbc'
UUID_EP3 = 'cae54555-7957-4d26-8515-7f2d1de5da55'
UUID_EP4 = '4bc83eb7-4147-435c-8e0f-a546288fd639'

def rpc_compose_fabric_uri():
    return "/restconf/operations/fabric:compose-fabric"

def rpc_compose_fabric_data():
    return {
      "input" : {
           "name": "first fabric",
           "type":"VLAN",
           "device-nodes" : [
             {
                "device-ref":"/network-topology:network-topology/network-topology:topology[network-topology:topology-id='CE']/network-topology:node[network-topology:node-id='192.168.1.141']",
                 "role":"SPINE"
              },{
                "device-ref":"/network-topology:network-topology/network-topology:topology[network-topology:topology-id='CE']/network-topology:node[network-topology:node-id='192.168.1.142']"
             },{
                "device-ref":"/network-topology:network-topology/network-topology:topology[network-topology:topology-id='CE']/network-topology:node[network-topology:node-id='192.168.1.143']"
             }
           ]
       }
    }


def rpc_create_logic_switch_uri():
    return "/restconf/operations/fabric-service:create-logical-switch"

def rpc_create_logic_switch_data(name, vni):
    return {
        "input" : {
           "fabric-id": "fabric:1",
           "name":name,
           "vni":vni
         }
    }

def rpc_rm_logic_switch_uri():
    return "/restconf/operations/fabric-service:rm-logical-switch"

def rpc_rm_logic_switch_data(name, vni):
    return {
        "input" : {
           "fabric-id": "fabric:1",
           "node-id":name
         }
    }

def rpc_create_logic_router_uri():
    return "/restconf/operations/fabric-service:create-logical-router"

def rpc_create_logic_router_data(name):
    return {
        "input" : {
           "fabric-id": "fabric:1",
           "name":name
        }
    }

def rpc_rm_logic_router_uri():
    return "/restconf/operations/fabric-service:rm-logical-router"

def rpc_rm_logic_router_data(name):
    return {
        "input" : {
           "fabric-id": "fabric:1",
           "node-id":name
        }
    }

def rpc_create_logic_port_uri():
    return "/restconf/operations/fabric-service:create-logical-port"

def rpc_create_logic_port_data(deviceName, portName):
    return {
        "input" : {
           "fabric-id": "fabric:1",
           "name":portName,
           "logical-device":deviceName
       }
    }

def rpc_register_endpoint_uri():
    return "/restconf/operations/fabric-endpoint:register-endpoint"

def rpc_register_endpoint_data1():
    return {
        "input" : {
           "fabric-id": "fabric:1",
           "endpoint-uuid":UUID_EP1,
           "mac-address":"00:0c:29:ae:27:24",
           "ip-address":"172.16.1.2",
           "gateway":"172.16.1.1",
            "logical-location": {
                "node-id":"vswitch-1",
                "tp-id":"vswitch-1-p-1"
            }
       }
    }

def rpc_register_endpoint_data2():
    return {
        "input" : {
           "fabric-id": "fabric:1",
           "endpoint-uuid":UUID_EP2,
           "mac-address":"00:0c:29:4e:2b:81",
           "ip-address":"172.16.2.2",
           "gateway":"172.16.2.1",
            "logical-location": {
                "node-id":"vswitch-2",
                "tp-id":"vswitch-2-p-1"
            }
       }
    }

def rpc_register_endpoint_data3():
    return {
        "input" : {
           "fabric-id": "fabric:1",
           "endpoint-uuid":UUID_EP3,
           "mac-address":"00:0c:29:b2:99:65",
           "ip-address":"172.16.3.2",
           "gateway":"172.16.3.1",
            "logical-location": {
                "node-id":"vswitch-3",
                "tp-id":"vswitch-3-p-1"
            }
       }
    }

def rpc_unregister_endpoint_uri():
    return "/restconf/operations/fabric-endpoint:unregister-endpoint"

def rpc_unregister_endpoint_data():
    return {
        "input" : {
           "fabric-id": "fabric:1",
           "ids":(UUID_EP1,UUID_EP2,UUID_EP3,UUID_EP4)
       }
    }


def rpc_locate_endpoint_uri():
    return "/restconf/operations/fabric-endpoint:locate-endpoint"

def rpc_locate_endpoint_data1():
    return {
      "input" : {
           "fabric-id": "fabric:1",
           "endpoint-id":UUID_EP1,
            "location": {
                "node-ref":"/network-topology:network-topology/network-topology:topology[network-topology:topology-id='CE']/network-topology:node[network-topology:node-id='192.168.1.142']",
                "tp-ref":"/network-topology:network-topology/network-topology:topology[network-topology:topology-id='CE']/network-topology:node[network-topology:node-id='192.168.1.142']/network-topology:termination-point[network-topology:tp-id='10GE1/0/35']",
                "access-type":"vlan",
                "access-segment":101
            }
       }
    }

def rpc_locate_endpoint_data2():
    return {
      "input" : {
           "fabric-id": "fabric:1",
           "endpoint-id":UUID_EP2,
            "location": {
                "node-ref":"/network-topology:network-topology/network-topology:topology[network-topology:topology-id='CE']/network-topology:node[network-topology:node-id='192.168.1.142']",
                "tp-ref":"/network-topology:network-topology/network-topology:topology[network-topology:topology-id='CE']/network-topology:node[network-topology:node-id='192.168.1.142']/network-topology:termination-point[network-topology:tp-id='10GE1/0/25']",
                "access-type":"vlan",
                "access-segment":102
            }
       }
    }

def rpc_locate_endpoint_data3():
    return {
      "input" : {
           "fabric-id": "fabric:1",
           "endpoint-id":UUID_EP3,
            "location": {
                "node-ref":"/network-topology:network-topology/network-topology:topology[network-topology:topology-id='CE']/network-topology:node[network-topology:node-id='192.168.1.143']",
                "tp-ref":"/network-topology:network-topology/network-topology:topology[network-topology:topology-id='CE']/network-topology:node[network-topology:node-id='192.168.1.143']/network-topology:termination-point[network-topology:tp-id='10GE1/0/25']",
                "access-type":"vlan",
                "access-segment":103
            }
       }
    }

def rpc_port_binding_uri():
    return "/restconf/operations/fabric-service:port-binding-logical-to-fabric"

def rpc_port_binding_data3():
    return {
      "input" : {
           "fabric-id": "fabric:1",
           "logical-device":"vswitch-1",
           "logical-port":"vswitch-1-p-3",
           "fabric-port": NODE_ID_OVSDB + "/bridge/s2 - s2-eth1"
       }
    }

def rpc_port_binding_dev_uri():
    return "/restconf/operations/fabric-service:port-binding-logical-to-device"

def rpc_port_binding_dev_data(logicalDev, logicalPort, physicalDev, physicalPort):
    return {
      "input" : {
           "fabric-id": "fabric:1",
           "logical-device":logicalDev,
           "logical-port":logicalPort,
           "physical-port":"/network-topology:network-topology/network-topology:topology[network-topology:topology-id='CE']/network-topology:node[network-topology:node-id='" + physicalDev + "']/network-topology:termination-point[network-topology:tp-id='" + physicalPort + "']"
       }
    }

def rpc_create_gateway_uri():
    return "/restconf/operations/fabric-service:create-gateway"

def rpc_create_gateway_data(ipaddr, network, switchName):
    return {
      "input" : {
           "fabric-id": "fabric:1",
           "ip-address":ipaddr,
           "network":network,
           "logical-router":"vrouter-1",
           "logical-switch":switchName
       }
    }

def rpc_rm_gateway_uri():
    return "/restconf/operations/fabric-service:rm-gateway"

def rpc_rm_gateway_data(ipaddr):
    return {
      "input" : {
           "fabric-id": "fabric:1",
           "ip-address":ipaddr,
           "logical-router":"vrouter-1"
       }
    }

def pause():
    print "press Enter key to continue..."
    raw_input()

if __name__ == "__main__":
    # Launch main menu


    # Some sensible defaults
    controller = os.environ.get('ODL')
    if controller == None:
        sys.exit("No controller set.")

    print "compose fabric"
    post(controller, DEFAULT_PORT, rpc_compose_fabric_uri(), rpc_compose_fabric_data(), True)

    print "create_logic_switch ..."
    pause()
    post(controller, DEFAULT_PORT, rpc_create_logic_switch_uri(), rpc_create_logic_switch_data("vswitch-1", 1), True)
    post(controller, DEFAULT_PORT, rpc_create_logic_switch_uri(), rpc_create_logic_switch_data("vswitch-2", 2), True)
    post(controller, DEFAULT_PORT, rpc_create_logic_switch_uri(), rpc_create_logic_switch_data("vswitch-3", 3), True)

    print "create_logic_router ..."
    pause() 
    post(controller, DEFAULT_PORT, rpc_create_logic_router_uri(), rpc_create_logic_router_data("vrouter-1"), True)

    print "create_logic_port ..."
    pause()
    post(controller, DEFAULT_PORT, rpc_create_logic_port_uri(), rpc_create_logic_port_data("vswitch-1", "vswitch-1-p-1"), True)
    post(controller, DEFAULT_PORT, rpc_create_logic_port_uri(), rpc_create_logic_port_data("vswitch-2", "vswitch-2-p-1"), True)
    post(controller, DEFAULT_PORT, rpc_create_logic_port_uri(), rpc_create_logic_port_data("vswitch-3", "vswitch-3-p-1"), True)


    print "registering endpoints ..."
    pause()
    post(controller, DEFAULT_PORT, rpc_register_endpoint_uri(), rpc_register_endpoint_data1(), True)
    post(controller, DEFAULT_PORT, rpc_register_endpoint_uri(), rpc_register_endpoint_data2(), True)
    post(controller, DEFAULT_PORT, rpc_register_endpoint_uri(), rpc_register_endpoint_data3(), True)

    print "locate endpoints ..."
    pause()
    post(controller, DEFAULT_PORT, rpc_locate_endpoint_uri(), rpc_locate_endpoint_data1(), True)
    post(controller, DEFAULT_PORT, rpc_locate_endpoint_uri(), rpc_locate_endpoint_data2(), True)
    post(controller, DEFAULT_PORT, rpc_locate_endpoint_uri(), rpc_locate_endpoint_data3(), True)

    print "binding physical port"
    pause()
    #post(controller, DEFAULT_PORT, rpc_port_binding_dev_uri(), rpc_port_binding_dev_data("vswitch-1", "vswitch-1-p-1", "192.168.1.142", "10GE1/0/35"), True)
    #post(controller, DEFAULT_PORT, rpc_port_binding_dev_uri(), rpc_port_binding_dev_data("vswitch-2", "vswitch-2-p-1", "192.168.1.142", "10GE1/0/25"), True)
    #post(controller, DEFAULT_PORT, rpc_port_binding_dev_uri(), rpc_port_binding_dev_data("vswitch-3", "vswitch-3-p-1", "192.168.1.143", "10GE1/0/25"), True)

    print "create gateway ..."
    pause()
    post(controller, DEFAULT_PORT, rpc_create_gateway_uri(), rpc_create_gateway_data("172.16.1.1", "172.16.1.0/24", "vswitch-1"), True)
    post(controller, DEFAULT_PORT, rpc_create_gateway_uri(), rpc_create_gateway_data("172.16.2.1", "172.16.2.0/24", "vswitch-2"), True)
    post(controller, DEFAULT_PORT, rpc_create_gateway_uri(), rpc_create_gateway_data("172.16.3.1", "172.16.3.0/24", "vswitch-3"), True)

