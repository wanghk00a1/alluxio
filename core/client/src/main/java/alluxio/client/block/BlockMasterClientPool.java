/*
 * The Alluxio Open Foundation licenses this work under the Apache License, version 2.0
 * (the "License"). You may not use this work except in compliance with the License, which is
 * available at www.apache.org/licenses/LICENSE-2.0
 *
 * This software is distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied, as more fully set forth in the License.
 *
 * See the NOTICE file distributed with this work for information regarding copyright ownership.
 */

package alluxio.client.block;

import alluxio.Configuration;
import alluxio.PropertyKey;
import alluxio.resource.ResourcePool;

import java.net.InetSocketAddress;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.annotation.concurrent.ThreadSafe;
import javax.security.auth.Subject;

/**
 * Class for managing block master clients. After obtaining a client with
 * {@link ResourcePool#acquire()}, {@link ResourcePool#release(Object)} must be called when the
 * thread is done using the client.
 */
@ThreadSafe
public final class BlockMasterClientPool extends ResourcePool<BlockMasterClient> {
  private final InetSocketAddress mMasterAddress;
  private final Queue<BlockMasterClient> mClientList;
  private final Subject mSubject;

  /**
   * Creates a new block master client pool.
   *
   * @param subject the parent subject
   * @param masterAddress the master address
   */
  public BlockMasterClientPool(Subject subject, InetSocketAddress masterAddress) {
    super(Configuration.getInt(PropertyKey.USER_BLOCK_MASTER_CLIENT_THREADS));
    mSubject = subject;
    mMasterAddress = masterAddress;
    mClientList = new ConcurrentLinkedQueue<>();
  }

  /**
   * Creates a new block master client pool.
   *
   * @param subject the parent subject
   * @param masterAddress the master address
   * @param clientThreads the number of client threads to use
   */
  public BlockMasterClientPool(Subject subject, InetSocketAddress masterAddress,
      int clientThreads) {
    super(clientThreads);
    mSubject = subject;
    mMasterAddress = masterAddress;
    mClientList = new ConcurrentLinkedQueue<>();
  }

  @Override
  public void close() {
    BlockMasterClient client;
    while ((client = mClientList.poll()) != null) {
      client.close();
    }
  }

  @Override
  protected BlockMasterClient createNewResource() {
    return RetryHandlingBlockMasterClient.create(mSubject, mMasterAddress);
  }
}
