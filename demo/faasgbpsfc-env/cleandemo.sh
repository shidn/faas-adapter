#!/usr/bin/env bash


for i in `seq 1 $NUM_NODES`; do
  hostname="box"$i
  echo $hostname
  vagrant ssh $hostname -c "sudo ovs-vsctl list-br | xargs -I {} sudo ovs-vsctl del-br {}; sudo ovs-vsctl del-manager; sudo /vagrant/vmclean.sh  >/dev/null 2>&1"

done

./rest-clean.py

if [ -f "demo.lock" ] ; then
  rm demo.lock
fi
