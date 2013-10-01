package org.apache.hadoop.hdfs.server.namenode;

import java.util.*;

import org.apache.hadoop.net.NetworkTopology;
import org.apache.hadoop.net.Node;
import org.apache.hadoop.net.NodeBase;
import org.apache.commons.logging.*;
import org.apache.hadoop.hdfs.protocol.DatanodeInfo;
import org.apache.hadoop.hdfs.protocol.FSConstants;
import org.apache.hadoop.hdfs.protocol.LocatedBlock;

/** The class is responsible for choosing the desired number of targets
 * for placing block replicas. The core idea is that we now consider
 * placing replicas across *multiple datacenters*.
 *
 * The replica placement strategy is that if the writer is on a datanode,
 * the 1st replica is placed on the local machine,
 * otherwise a random datanode within the same datacenter as writer.
 * The 2nd replica is placed on a datanode
 * that is on a different rack within the same datacenter as 1st replica.
 * The 3rd replica is placed on a datanode
 * which is on a random datacenter different from the 1st and 2nd replicas.
 */
public class DatacenterAwareTargetChooser {

    //
    // Untouched code  ====>
    //
    protected final boolean considerLoad;
    protected NetworkTopology clusterMap;
    protected FSNamesystem fs;

    DatacenterAwareTargetChooser(boolean considerLoad, FSNamesystem fs,
                                 NetworkTopology clusterMap) {
        this.considerLoad = considerLoad;
        this.fs = fs;
        this.clusterMap = clusterMap;
    }

    protected static class NotEnoughReplicasException extends Exception {
        NotEnoughReplicasException(String msg) {
            super(msg);
        }
    }

    /**
     * choose <i>numOfReplicas</i> data nodes for <i>writer</i> to replicate
     * a block with size <i>blocksize</i>
     * If not, return as many as we can.
     *
     * @param numOfReplicas: number of replicas wanted.
     * @param writer: the writer's machine, null if not in the cluster.
     * @param excludedNodes: datanodesthat should not be considered targets.
     * @param blocksize: size of the data to be written.
     * @return array of DatanodeDescriptor instances chosen as targets
     * and sorted as a pipeline.
     */
    DatanodeDescriptor[] chooseTarget(int numOfReplicas,
                                      DatanodeDescriptor writer,
                                      List<Node> excludedNodes,
                                      long blocksize) {
        if (excludedNodes == null) {
            excludedNodes = new ArrayList<Node>();
        }

        return chooseTarget(numOfReplicas, writer,
                new ArrayList<DatanodeDescriptor>(), excludedNodes, blocksize);
    }

    /**
     * choose <i>numOfReplicas</i> data nodes for <i>writer</i>
     * to re-replicate a block with size <i>blocksize</i>
     * If not, return as many as we can.
     *
     * @param numOfReplicas: additional number of replicas wanted.
     * @param writer: the writer's machine, null if not in the cluster.
     * @param choosenNodes: datanodes that have been choosen as targets.
     * @param excludedNodes: datanodesthat should not be considered targets.
     * @param blocksize: size of the data to be written.
     * @return array of DatanodeDescriptor instances chosen as target
     * and sorted as a pipeline.
     */
    DatanodeDescriptor[] chooseTarget(int numOfReplicas,
                                      DatanodeDescriptor writer,
                                      List<DatanodeDescriptor> choosenNodes,
                                      List<Node> excludedNodes,
                                      long blocksize) {
        if (numOfReplicas == 0 || clusterMap.getNumOfLeaves()==0) {
            return new DatanodeDescriptor[0];
        }

        if (excludedNodes == null) {
            excludedNodes = new ArrayList<Node>();
        }

        int clusterSize = clusterMap.getNumOfLeaves();
        int totalNumOfReplicas = choosenNodes.size()+numOfReplicas;
        if (totalNumOfReplicas > clusterSize) {
            numOfReplicas -= (totalNumOfReplicas-clusterSize);
            totalNumOfReplicas = clusterSize;
        }

        int maxNodesPerRack =
                (totalNumOfReplicas-1)/clusterMap.getNumOfRacks()+2;

        List<DatanodeDescriptor> results =
                new ArrayList<DatanodeDescriptor>(choosenNodes);
        excludedNodes.addAll(choosenNodes);

        if (!clusterMap.contains(writer)) {
            writer=null;
        }

        DatanodeDescriptor localNode = chooseTarget(numOfReplicas, writer,
                excludedNodes, blocksize, maxNodesPerRack, results);

        results.removeAll(choosenNodes);

        // sorting nodes to form a pipeline
        return getPipeline((writer==null)?localNode:writer,
                results.toArray(new DatanodeDescriptor[results.size()]));
    }

    

    //
    // Code block for elf
    //

    /*
     * This is the function where the placement really happens
     * Impl the placement policy for multi-datacenter here
     * Refer to the impl in ReplicationTargetChooser.java: line 134 for old impl
     * */
    private DatanodeDescriptor chooseTarget(int numOfReplicas,
                                            DatanodeDescriptor writer,
                                            List<Node> excludedNodes,
                                            long blocksize,
                                            int maxNodesPerRack,
                                            List<DatanodeDescriptor> results) {
        //TODO: implement me
    	if (numOfReplicas == 0 || clusterMap.getNumOfLeaves() == 0) {
    		return writer;
    	}
    	
    	int numOfResults = results.size();
    	boolean newBlock = (numOfResults == 0);
    	if (writer == null && !newBlock) {
    		writer = (DatanodeDescriptor)results.get(0);
    	}
    	Log logr = FSNamesystem.LOG;
		
    	try {
    		switch(numOfResults) {
    		case 0:
    			writer = chooseLocalNode(writer, excludedNodes,
    									blocksize, maxNodesPerRack, results);
    			logr.info("Case 0: " + writer.getNetworkLocation());
    			logr.info("Case 0 (results(0)): " + results.get(0).getNetworkLocation());
    			if (--numOfReplicas == 0) {
    				break;
    			}
    			/*case 1:
    			chooseRemoteRack(1, results.get(0), excludedNodes, blocksize,
    							maxNodesPerRack, results);
    			if (--numOfReplicas == 0) {
    				break;
    			}*/
    		//case 1:
    		case 1:
    			chooseRemoteDC(1, results.get(0), excludedNodes, blocksize, 
    							maxNodesPerRack, results);
    			logr.info("Case 1 (results(1)): " + results.get(1).getNetworkLocation());
    			if (--numOfReplicas == 0) {
    				break;
    			}
    			// may have more things to handle, refer to ReplicationTargetChooser.java
    		case 2:
    			if (clusterMap.isInSameDC(results.get(0), results.get(1))) {
    				chooseRemoteDC(1, results.get(0), excludedNodes, blocksize, 
							maxNodesPerRack, results);
    				logr.info("first two on same rack!");
    			}
    			else {
    				chooseRemoteRack(1, results.get(1), excludedNodes, blocksize,
							maxNodesPerRack, results);
    				logr.info("first two on different rack!");
    			}
    			logr.info("Case 2 (results(2)): " + results.get(2).getNetworkLocation());
    			if (--numOfReplicas == 0) {
    				break;
    			}
    			    		
    		default:
    			chooseRandom(numOfReplicas, NodeBase.ROOT, excludedNodes,
    						blocksize, maxNodesPerRack, results);
    		}
    	} catch (NotEnoughReplicasException e) {
    		FSNamesystem.LOG.warn("Not able to place enough replicas, still in need of "
    				+ numOfReplicas);
    	}
        return writer;
    }
    
    private DatanodeDescriptor chooseLocalNode(DatanodeDescriptor localMachine,
    											List<Node> excludedNodes,
    											long blocksize,
    											int maxNodesPerRack,
    											List<DatanodeDescriptor> results)
    throws NotEnoughReplicasException {
		// if no local Machine, randomly choose a node
    	if (localMachine == null) {
    		return chooseRandom(NodeBase.ROOT, excludedNodes,
    							blocksize, maxNodesPerRack, results); 
    	}
    	// otherwise, choose local machine first
    	if(!excludedNodes.contains(localMachine)) {
    		excludedNodes.add(localMachine);
    		if(isGoodTarget(localMachine, blocksize,
    						maxNodesPerRack, false, results)) {
    			results.add(localMachine);
    			return localMachine;
    		}
    	}
    	// try local rack
    	return chooseLocalRack(localMachine, excludedNodes,
    							blocksize, maxNodesPerRack, results);
	}
    
	
    private DatanodeDescriptor chooseLocalRack(DatanodeDescriptor localMachine,
                                             	List<Node> excludedNodes,
                                             	long blocksize,
                                             	int maxNodesPerRack,
                                             	List<DatanodeDescriptor> results)
    throws NotEnoughReplicasException {
		// no local machine, return a random node
    	if (localMachine == null) {
    		return chooseRandom(NodeBase.ROOT, excludedNodes,
								blocksize, maxNodesPerRack, results); 
    	}
    	
    	// otherwise, choose from local rack
    	try {
    		return chooseRandom(localMachine.getNetworkLocation(),
    								excludedNodes, blocksize, maxNodesPerRack, results);
    	} catch (NotEnoughReplicasException e1) {
    		// find the next replica
    		DatanodeDescriptor newLocal=null;
    		for (Iterator<DatanodeDescriptor> iter = results.iterator();
    				iter.hasNext(); ) {
    			DatanodeDescriptor nextNode = iter.next();
    			if (nextNode != localMachine) {
    				newLocal = nextNode;
    				break;
    			}
    		}
    		if (newLocal != null) {
    			try {
    				return chooseRandom(newLocal.getNetworkLocation(), excludedNodes,
    									blocksize, maxNodesPerRack, results);
    			} catch (NotEnoughReplicasException e2) {
    				// the second failed, so random
    				return chooseRandom(NodeBase.ROOT, excludedNodes,
										blocksize, maxNodesPerRack, results);
    			}
    		} else {
    			// no other nodes, random
    			return chooseRandom(NodeBase.ROOT, excludedNodes,
									blocksize, maxNodesPerRack, results); 
    		}
    	}
	}

	private void chooseRemoteRack(int numOfReplicas, 
									DatanodeDescriptor localMachine,
									List<Node> excludedNodes, 
									long blocksize, 
									int maxNodesPerRack,
									List<DatanodeDescriptor> results) 
	throws NotEnoughReplicasException {
		int oldNumOfReplicas = results.size();
		// choose from remote rack
		// if not enough remote rack, choose the rest from local rack
		try {
			Log logr = FSNamesystem.LOG;
			logr.info("The getNetworkLocation is: " + localMachine.getNetworkLocation());
			String dc = (localMachine.getParent().getParent()).getNetworkLocation()
					+ '/' + (localMachine.getParent().getParent()).getName();
			chooseRandom(numOfReplicas, dc, "~"+localMachine.getNetworkLocation(),
							excludedNodes, blocksize, maxNodesPerRack, results);
		} catch (NotEnoughReplicasException e) {
			chooseRandom(numOfReplicas-(results.size()-oldNumOfReplicas),
							localMachine.getNetworkLocation(), excludedNodes, 
							blocksize, maxNodesPerRack, results);
		}
	}

	private void chooseRemoteDC(int numOfReplicas,
								DatanodeDescriptor localMachine,
								List<Node> excludedNodes, 
								long blocksize, 
								int maxNodesPerRack,
								List<DatanodeDescriptor> results)
	throws NotEnoughReplicasException {
		// choose from remote data center
		// if not enough, choose from remote rack under the same data center
		//String dc = (localMachine.getParent().getParent()).getName();//test!
		String dc = (localMachine.getParent().getParent()).getNetworkLocation()
						+ '/' + (localMachine.getParent().getParent()).getName();
		Log logr = FSNamesystem.LOG;
		logr.info("The Datacenter name is: " + dc);
		//logr.info("The Datacenter location is: " + location);
		int oldNumOfReplicas = results.size();
		try {
			chooseRandom(numOfReplicas, "~"+dc, excludedNodes, blocksize,
							maxNodesPerRack, results);
		} catch (NotEnoughReplicasException e) {
			chooseRemoteRack(numOfReplicas-(results.size()-oldNumOfReplicas),
					localMachine, excludedNodes, 
					blocksize, maxNodesPerRack, results);
		}		
	}

	/* Randomly choose one target from <i>nodes</i>.
	 * @return the chosen node
	 */
	private DatanodeDescriptor chooseRandom(String nodes,
            								List<Node> excludedNodes,
            								long blocksize,
            								int maxNodesPerRack,
            								List<DatanodeDescriptor> results) 
    throws NotEnoughReplicasException {
		// choose 1 from nodes
		DatanodeDescriptor result;
		do {
			DatanodeDescriptor[] selectedNodes = 
					chooseRandom(1, nodes, excludedNodes);
			if (selectedNodes.length == 0) {
				throw new NotEnoughReplicasException(
						"Not able to place enough replicas");
			}
			result = (DatanodeDescriptor)selectedNodes[0];
		} while (!isGoodTarget(result, blocksize, maxNodesPerRack, results));
		results.add(result);
		return result;
	}

	/* Randomly choose <i>numOfReplicas</i> targets from <i>nodes</i>.
	 */
	private void chooseRandom(int numOfReplicas,
            					String nodes,
            					List<Node> excludedNodes,
            					long blocksize,
            					int maxNodesPerRack,
            					List<DatanodeDescriptor> results)
    throws NotEnoughReplicasException {
		// 
		boolean toContinue = true;
		do {
			DatanodeDescriptor[] selectedNodes = 
					chooseRandom(numOfReplicas, nodes, excludedNodes);
			if (selectedNodes.length < numOfReplicas) {
				toContinue = false;
			}
			for (int i = 0; i < selectedNodes.length; i++) {
				DatanodeDescriptor result = selectedNodes[i];
				if (isGoodTarget(result, blocksize, maxNodesPerRack, results)) {
					numOfReplicas--;
					results.add(result);
				}
			}
		} while (numOfReplicas > 0 && toContinue);
		
		if (numOfReplicas > 0) {
			throw new NotEnoughReplicasException(
					"Not able to place enough replicas");
		}
	}
	
	/* Randomly choose <i>numOfReplicas</i> targets from <i>root</i>,
	 * exclude <i>excludednodesnodes</i>.
	 */
	private void chooseRandom(int numOfReplicas,
								String root,
            					String excludednodes,
            					List<Node> excludedNodes,
            					long blocksize,
            					int maxNodesPerRack,
            					List<DatanodeDescriptor> results)
    throws NotEnoughReplicasException {
		boolean toContinue = true;
		do {
			DatanodeDescriptor[] selectedNodes = 
					chooseRandom(numOfReplicas, root, excludednodes, excludedNodes);
			if (selectedNodes.length < numOfReplicas) {
				toContinue = false;
			}
			for (int i = 0; i < selectedNodes.length; i++) {
				DatanodeDescriptor result = selectedNodes[i];
				if (isGoodTarget(result, blocksize, maxNodesPerRack, results)) {
					numOfReplicas--;
					results.add(result);
				}
			}
		} while (numOfReplicas > 0 && toContinue);
		
		if (numOfReplicas > 0) {
			throw new NotEnoughReplicasException(
					"Not able to place enough replicas");
		}
	}

	/* Randomly choose <i>numOfNodes</i> nodes from <i>scope</i>.
	 * @return the chosen nodes
	 */
	private DatanodeDescriptor[] chooseRandom(int numOfReplicas, 
												String nodes,
												List<Node> excludedNodes) {
		List<DatanodeDescriptor> results =
				new ArrayList<DatanodeDescriptor>();
		int numOfAvailableNodes = 
				clusterMap.countNumOfAvailableNodes(nodes, excludedNodes);
		numOfReplicas = (numOfAvailableNodes < numOfReplicas) ? 
				numOfAvailableNodes : numOfReplicas;
		
		while (numOfReplicas > 0) {
			DatanodeDescriptor chosenNode = 
					(DatanodeDescriptor)(clusterMap.chooseRandom(nodes));
			if (!excludedNodes.contains(chosenNode)) {
				results.add(chosenNode);
				excludedNodes.add(chosenNode);
				numOfReplicas--;
			}
		}
		return (DatanodeDescriptor[])results.toArray(
				new DatanodeDescriptor[results.size()]);
	}
	
	/* Randomly choose <i>numOfNodes</i> nodes from <i>root</i>,
	 * exclude <i>excludednodesnodes</i>
	 * @return the chosen nodes
	 */
	private DatanodeDescriptor[] chooseRandom(int numOfReplicas, 
												String root,
												String excludednodes,
												List<Node> excludedNodes) {
		// 
		List<DatanodeDescriptor> results = 
				new ArrayList<DatanodeDescriptor>();
		int numOfAvailableNodes = 
				clusterMap.countNumOfAvailableNodes(root, excludedNodes);
		numOfReplicas = (numOfAvailableNodes < numOfReplicas) ? 
				numOfAvailableNodes : numOfReplicas;
		
		while (numOfReplicas > 0) {
			DatanodeDescriptor chosenNode = 
					(DatanodeDescriptor)(clusterMap.chooseRandomFrom(root, excludednodes));
			if (!excludedNodes.contains(chosenNode)) {
				results.add(chosenNode);
				excludedNodes.add(chosenNode);
				numOfReplicas--;
			}
		}
		return (DatanodeDescriptor[])results.toArray(
				new DatanodeDescriptor[results.size()]);
	}

	/* Return a pipeline of nodes.
     * The pipeline is formed finding a shortest path that
     * starts from the writer and tranverses all <i>nodes</i>
     * This is basically a traveling salesman problem.
     */
    private DatanodeDescriptor[] getPipeline(
            DatanodeDescriptor writer,
            DatanodeDescriptor[] nodes) {
        if (nodes.length==0) return nodes;

        synchronized(clusterMap) {
            int index=0;
            if (writer == null || !clusterMap.contains(writer)) {
                writer = nodes[0];
            }
            for(;index<nodes.length; index++) {
                DatanodeDescriptor shortestNode = nodes[index];
                int shortestDistance = clusterMap.getDistance(writer, shortestNode);
                int shortestIndex = index;
                for(int i=index+1; i<nodes.length; i++) {
                    DatanodeDescriptor currentNode = nodes[i];
                    int currentDistance = clusterMap.getDistance(writer, currentNode);
                    if (shortestDistance>currentDistance) {
                        shortestDistance = currentDistance;
                        shortestNode = currentNode;
                        shortestIndex = i;
                    }
                }
                //switch position index & shortestIndex
                if (index != shortestIndex) {
                    nodes[shortestIndex] = nodes[index];
                    nodes[index] = shortestNode;
                }
                writer = shortestNode;
            }
        }
        return nodes;
    }
    

    /* judge if a node is a good target.
     * return true if <i>node</i> has enough space, 
     * does not have too much load, and the rack does not have too many nodes
     */
    private boolean isGoodTarget(DatanodeDescriptor node,
                                 long blockSize, int maxTargetPerLoc,
                                 List<DatanodeDescriptor> results) {
      return isGoodTarget(node, blockSize, maxTargetPerLoc,
                          this.considerLoad, results);
    }
    
    private boolean isGoodTarget(DatanodeDescriptor node,
            						long blockSize, int maxTargetPerLoc,
            						boolean considerLoad,
            						List<DatanodeDescriptor> results) {
		Log logr = FSNamesystem.LOG;
		//check if the node is (being) decommissioned
		if (node.isDecommissionInProgress() || node.isDecommissioned()) {
			logr.debug("Node "+NodeBase.getPath(node)+
	                " is not chosen because the node is (being) decommissioned");
		      return false;
		}
		
		long remaining = node.getRemaining() - 
								(node.getBlocksScheduled() * blockSize);
		
		// check the remaining capacity of the target machine
		if(blockSize * FSConstants.MIN_BLOCKS_FOR_WRITE > remaining) {
			logr.debug("Node "+NodeBase.getPath(node)+
	                " is not chosen because the node does not have enough space");
		      return false;
		}
		
		// check the communication traffic of the target machine
		if (considerLoad) {
			double avgLoad = 0;
			int size = clusterMap.getNumOfLeaves();
			if (size != 0) {
				avgLoad = (double)fs.getTotalLoad()/size;
			}
			if (node.getXceiverCount() > (2.0 * avgLoad)) {
				logr.debug("Node "+NodeBase.getPath(node)+
		                  " is not chosen because the node is too busy");
		        return false;
			}
		}
		
		// check if the target rack has chosen too many nodes
		String rackname = node.getNetworkLocation();
		int counter = 1;
		for (Iterator<DatanodeDescriptor> iter = results.iterator();
				iter.hasNext();) {
			Node result = iter.next();
			if (rackname.equals(result.getNetworkLocation())) {
				counter++;
			}
		}
		if (counter > maxTargetPerLoc) {
			logr.debug("Node "+NodeBase.getPath(node)+
	                " is not chosen because the rack has too many chosen nodes");
			return false;
	    }
	    return true;
	}

	/**
     * The verify policy is now a little bit more complex than b4, when we just
     * need to verify that we replica a block on two racks at minimum,
     * if only we have more than one rack.
     *
     * Now we need to
     *  - 1) first verify that the one block is replicated in
     *  at least two different datacenters if there are more than one,
     *  - 2) then verify in the one that has two replicas, we didn't put them inside
     *  the same rack
     *
     * @return 0 if the placement is correct, 1 if condition 1) is violated, 2 if
     *  condition 2) is violated
     */
    public static int verifyBlockPlacement(LocatedBlock lBlk,
                                           short replication,
                                           NetworkTopology cluster) {
        //
    	int numDCs = verifyBlockPlacement(lBlk, Math.min(2, replication), cluster);
        return numDCs < 0 ? 0 : numDCs;
    }

	private static int verifyBlockPlacement(LocatedBlock lBlk, 
											int minDCs,
											NetworkTopology cluster) {
		// 
		DatanodeInfo[] locs = lBlk.getLocations();
		if (locs == null)
			locs = new DatanodeInfo[0];
		int numDCs = cluster.getNumOfDC();
		if (numDCs <= 1)
			return 0;
		minDCs = Math.min(minDCs, numDCs);
		// 1. check that all location are different
		// 2. count locations on different DCs.
		Set<String> dcs = new TreeSet<String>();
		for (DatanodeInfo dn : locs)
			dcs.add(dn.getParent().getParent().getName()); // test
		return minDCs - dcs.size();
	}
}
