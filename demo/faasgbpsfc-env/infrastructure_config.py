# Config for switches.
switches = [
            {'name': 'sw11',
             'dpid' : '11',
             'host': 'box5',
             'fabric': "1",
             'vtep': '192.168.50.81'},
            {'name': 'sw12',
             'dpid': '12',
             'host': 'box6',
             'fabric': '1',
             'vtep': '192.168.50.82'},
            {'name': 'sw13',
             'dpid': '13',
             'host': 'box7',
             'fabric': '1',
             'vtep': '192.168.50.83'},
            {'name': 'sw1',
             'dpid': '1',
             'host': 'box8',
             'fabric': '1',
             'vtep': '192.168.50.84'},
            {'name': 'sw2',
             'dpid': '2',
             'host': 'box8',
             'fabric': '2',
             'vtep': '192.168.50.85'},
            {'name': 'sw3',
             'dpid': '3',
             'host': 'box8',
             'fabric': None},
            {'name': 'sw21',
             'dpid': '21',
             'host': 'box9',
             'fabric': '2',
             'vtep': '192.168.50.86'}
        ]

defaultContainerImage='alagalah/odlpoc_ovs230'

#

hosts = [{'name': 'h35_2',
          'mac': '00:00:00:00:35:02',
          'ip': '10.0.35.2/24',
          'switch': 'sw11'},
         {'name': 'h35_3',
          'ip': '10.0.35.3/24',
          'mac': '00:00:00:00:35:03',
          'switch': 'sw11'},
         {'name': 'h35_4',
          'ip': '10.0.35.4/24',
          'mac': '00:00:00:00:35:04',
          'switch': 'sw12'},
         {'name': 'h35_5',
          'ip': '10.0.35.5/24',
          'mac': '00:00:00:00:35:05',
          'switch': 'sw12'},
         {'name': 'h36_2',
          'ip': '10.0.36.2/24',
          'mac': '00:00:00:00:36:02',
          'switch': 'sw11'},
         {'name': 'h36_3',
          'ip': '10.0.36.3/24',
          'mac': '00:00:00:00:36:03',
          'switch': 'sw11'},
         {'name': 'h36_4',
          'ip': '10.0.36.4/24',
          'mac': '00:00:00:00:36:04',
          'switch': 'sw12'},
         {'name': 'h36_5',
          'ip': '10.0.36.5/24',
          'mac': '00:00:00:00:36:05',
          'switch': 'sw12'},
         {'name':'h37_2',
          'ip': '10.0.37.2/24',
          'mac': '00:00:00:00:37:02',
          'switch': 'sw13'},
         {'name':'h37_3',
          'ip': '10.0.37.3/24',
          'mac': '00:00:00:00:37:03',
          'switch': 'sw13'},
#vm in fabric 2
         {'name': 'h35_8',
          'ip': '10.0.35.8/24',
          'mac': '00:00:00:00:35:08',
          'switch': 'sw21'},
         {'name': 'h36_8',
          'ip': '10.0.36.8/24',
          'mac': '00:00:00:00:36:08',
          'switch': 'sw21'},
         {'name': 'h37_8',
          'ip': '10.0.37.8/24',
          'mac': '00:00:00:00:37:08',
          'switch': 'sw21'},
#vm in outside network
         {'name':'hostx',
          'ip'  :'192.168.2.2/24',
          'mac' :'00:00:01:00:02:02',
          'switch': 'sw3'}
          ]

floating_ip_pool = [
        {'ip': '192.168.1.2',
         'fabric': '2'},
        {'ip': '192.168.1.3',
         'fabric': '2'},
        {'ip': '192.168.1.4',
         'fabric': '2'},
        {'ip': '192.168.1.5',
         'fabric': '2'}
    ]

floating_ip_gw_mac = '80:38:bC:A1:33:c8'

def getSwitchByName(name):
#    for switch in switches:
#        if switch['name'] == name:
#            return switch
    return filter(lambda x : x['name'] == name, switches)[0]

def getSwitchesByBox(box):
#    ret = list()
#    for switch in switches:
#        if switch['host'] == box:
#            ret.append(switch)
#    return ret
    return filter(lambda x : x['host'] == box, switches)

all_boxes = set()
def getAllBoxes():
    if not all_boxes:
        for switch in switches:
            all_boxes.add(switch['host'])
    return all_boxes
