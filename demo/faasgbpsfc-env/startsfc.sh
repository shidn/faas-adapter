#!/usr/bin/env bash

set -e

echo "Starting sfc:"

for i in `seq 1 $NUM_NODES`; do
  hostname="box"$i
  echo $hostname
  vagrant ssh $hostname -c "sudo -E /vagrant/sfc_lanuch.py"
done

echo "Configuring controller..."
./sfc_rest_initial.py
