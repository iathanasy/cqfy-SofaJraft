package com.alipay.sofa.jraft;

import com.alipay.sofa.jraft.conf.Configuration;
import com.alipay.sofa.jraft.entity.PeerId;
import com.alipay.sofa.jraft.option.CliOptions;

import java.util.List;
import java.util.Map;
import java.util.Set;


public interface CliService extends Lifecycle<CliOptions> {


    Status addPeer(final String groupId, final Configuration conf, final PeerId peer);


    Status removePeer(final String groupId, final Configuration conf, final PeerId peer);

    Status changePeers(final String groupId, final Configuration conf, final Configuration newPeers);


    Status resetPeer(final String groupId, final PeerId peer, final Configuration newPeers);


    Status addLearners(final String groupId, final Configuration conf, final List<PeerId> learners);


    Status removeLearners(final String groupId, final Configuration conf, final List<PeerId> learners);


    Status learner2Follower(final String groupId, final Configuration conf, final PeerId learner);


    Status resetLearners(final String groupId, final Configuration conf, final List<PeerId> learners);


    Status transferLeader(final String groupId, final Configuration conf, final PeerId peer);


    Status snapshot(final String groupId, final PeerId peer);


    Status getLeader(final String groupId, final Configuration conf, final PeerId leaderId);


    List<PeerId> getPeers(final String groupId, final Configuration conf);


    List<PeerId> getAlivePeers(final String groupId, final Configuration conf);


    List<PeerId> getLearners(final String groupId, final Configuration conf);


    List<PeerId> getAliveLearners(final String groupId, final Configuration conf);


    Status rebalance(final Set<String> balanceGroupIds, final Configuration conf,
                     final Map<String, PeerId> balancedLeaderIds);
}
