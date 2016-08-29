#SETUP

This is a demonstration/development environment for show-casing OpenDaylight Fabric as a Service (FAAS)

##steps of multi-fabric function tests.

1. `vargrant up`  
2. start controller  
    `feature:install odl-restconf odl-faas-vxlan-fabric odl-faas-vxlan-ovs-adapter`
3. `./startdemo.sh demo-multifabric`
4. `./composeFabric.py normal`
5. `./demo-multifabric/runFabricTest.py`

##steps of gbp intergration test on multi-fabric

1. `vagrant up`
2. start controller  
   `feature:install odl-restconf odl-sfcofl2 odl-sfc-ui odl-faas-all odl-groupbasedpolicy-ui odl-groupbasedpolicy-faas`
3. `startsfc.sh`
4. `startdemo.sh demo-multifabric`
5. `composeFabric.py`
6. run test script in faas folder  
   `cd ~/git/faas/demo/integration`  
   `./runTests.sh -t 3`
