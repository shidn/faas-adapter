#!/bin/bash


vagrant ssh box5 -c "sudo ovs-ofctl add-flow -OOpenFlow13 sw11 'table=10, priority=1024, reg0=0x2, actions=learn(table=110,priority=1024,NXM_NX_TUN_ID[],NXM_OF_ETH_DST[]=NXM_OF_ETH_SRC[],load:192->NXM_NX_TUN_IPV4_DST[0..7],load:168->NXM_NX_TUN_IPV4_DST[8..15],load:50->NXM_NX_TUN_IPV4_DST[16..23],load:84->NXM_NX_TUN_IPV4_DST[24..31],output:NXM_OF_IN_PORT[]),goto_table:20'"

vagrant ssh box6 -c "sudo ovs-ofctl add-flow -OOpenFlow13 sw12 'table=10, priority=1024, reg0=0x2, actions=learn(table=110,priority=1024,NXM_NX_TUN_ID[],NXM_OF_ETH_DST[]=NXM_OF_ETH_SRC[],load:192->NXM_NX_TUN_IPV4_DST[0..7],load:168->NXM_NX_TUN_IPV4_DST[8..15],load:50->NXM_NX_TUN_IPV4_DST[16..23],load:84->NXM_NX_TUN_IPV4_DST[24..31],output:NXM_OF_IN_PORT[]),goto_table:20'"

vagrant ssh box7 -c "sudo ovs-ofctl add-flow -OOpenFlow13 sw13 'table=10, priority=1024, reg0=0x2, actions=learn(table=110,priority=1024,NXM_NX_TUN_ID[],NXM_OF_ETH_DST[]=NXM_OF_ETH_SRC[],load:192->NXM_NX_TUN_IPV4_DST[0..7],load:168->NXM_NX_TUN_IPV4_DST[8..15],load:50->NXM_NX_TUN_IPV4_DST[16..23],load:84->NXM_NX_TUN_IPV4_DST[24..31],output:NXM_OF_IN_PORT[]),goto_table:20'"

vagrant ssh box9 -c "sudo ovs-ofctl add-flow -OOpenFlow13 sw21 'table=10, priority=1024, reg0=0x2, actions=learn(table=110,priority=1024,NXM_NX_TUN_ID[],NXM_OF_ETH_DST[]=NXM_OF_ETH_SRC[],load:192->NXM_NX_TUN_IPV4_DST[0..7],load:168->NXM_NX_TUN_IPV4_DST[8..15],load:50->NXM_NX_TUN_IPV4_DST[16..23],load:85->NXM_NX_TUN_IPV4_DST[24..31],output:NXM_OF_IN_PORT[]),goto_table:20'"

