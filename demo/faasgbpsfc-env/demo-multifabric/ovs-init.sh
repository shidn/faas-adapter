#!/bin/bash


vagrant ssh box5 -c "sudo ovs-ofctl add-flow -OOpenFlow13 sw11 'table=10, priority=1024, reg0=0x2, actions=learn(table=110,priority=1024,NXM_NX_TUN_ID[],NXM_OF_ETH_DST[]=NXM_OF_ETH_SRC[],load:0xc0a83254->NXM_NX_TUN_IPV4_DST[],output:NXM_OF_IN_PORT[]),goto_table:20'"

vagrant ssh box6 -c "sudo ovs-ofctl add-flow -OOpenFlow13 sw12 'table=10, priority=1024, reg0=0x2, actions=learn(table=110,priority=1024,NXM_NX_TUN_ID[],NXM_OF_ETH_DST[]=NXM_OF_ETH_SRC[],load:0xc0a83254->NXM_NX_TUN_IPV4_DST[],output:NXM_OF_IN_PORT[]),goto_table:20'"

vagrant ssh box7 -c "sudo ovs-ofctl add-flow -OOpenFlow13 sw13 'table=10, priority=1024, reg0=0x2, actions=learn(table=110,priority=1024,NXM_NX_TUN_ID[],NXM_OF_ETH_DST[]=NXM_OF_ETH_SRC[],load:0xc0a83254->NXM_NX_TUN_IPV4_DST[],output:NXM_OF_IN_PORT[]),goto_table:20'"

vagrant ssh box9 -c "sudo ovs-ofctl add-flow -OOpenFlow13 sw21 'table=10, priority=1024, reg0=0x2, actions=learn(table=110,priority=1024,NXM_NX_TUN_ID[],NXM_OF_ETH_DST[]=NXM_OF_ETH_SRC[],load:0xc0a83255->NXM_NX_TUN_IPV4_DST[],output:NXM_OF_IN_PORT[]),goto_table:20'"

