#SETUP

This is a demonstration/development environment for show-casing OpenDaylight Fabric as a Service (FAAS)

flow of multifabric tests.
1. vargrant up
2. start controller
    feature:install odl-restconf odl-sfcofl2 odl-sfc-ui odl-faas-all odl-groupbasedpolicy-ui odl-groupbasedpolicy-faas
3. ./startdemo.sh demo-multifabric
4. ./composeFabric.py normal
5. ./demo-multifabric/runFabricTest.py
