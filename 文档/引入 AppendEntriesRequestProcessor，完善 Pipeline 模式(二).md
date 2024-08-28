大家好，这一章我将继续为大家实现 Pipeline 模式传输日志的功能。在上一章的第二小节，我已经为大家把领导者发送日志传输请求的 Pipeline 模式实现了，接下来应该为大家实现领导者接收日志请求响应的 Pipeline 模式了。当然，这中间其实还有一个跟随者节点处理日志传输请求的 Pipeline 模式的实现，但这个还不着急讲，等我把领导者这边的 Pipeline 模式全实现了，我们再看看跟随者节点的代码怎么重构。

<a name="elnZP"></a>
## 梳理领导者处理跟随者节点响应全流程

大家应该还记得上一章我留下的几个问题，都是关于领导者处理日志请求响应方面的，**比如跟随者节点回复的响应怎么放到响应队列中，回复的响应用不用定义什么相关的对象来封装，毕竟要给这个响应设置响应序号，那这个响应序号要怎么设置。**还有最后特别重要的一个问题，**那就是当领导者接收到日志传输失败的响应后，应该只重复传输失败的这一批日志，还是从失败的日志开始，重新传输该批日志和之后所有的日志呢**？接下来，就让我为大家逐一解答这些问题。<br />不过再解答这几个问题之前，我想再罗嗦几句，因为我担心上一章有些知识没有讲清楚，让大家无法理解领导者究竟是怎么使用一个单线程执行器——也就是为 Replicator 复制器对象分配的单线程执行器——既发送日志传输请求又处理跟随者响应的，所以我想就这个问题再多说几句。当然，这样一来，就要从领导者日志传输的源头讲起了，所谓的源头，就是领导者向跟随者发送探针消息。<br />当领导者向跟随者发送了探针消息之后，领导者通过探针消息定位成功要发送给跟随者的下一条日志的索引，接下来领导者就会向跟随者直接发送探针消息。但是，让我们回过头想一下，领导者是怎么处理探针请求的响应的呢？当 raft 集群中一个节点成为领导之后，就会启动心跳定时器，然后向跟随者节点发送探针请求，具体流程如下，请看下面代码块。
```java
//整个jraft框架中第二核心类，该类的对象就是一个复制器，如果当前节点是领导者，那么集群中的其他节点都会被领导者包装成一个个
//复制器对象，心跳检测，日志传输都是通过复制器对象进行的
public class Replicator implements ThreadId.OnError {


    //省略其他内容

    //发送心跳或者探针请求的方法，根据方法中的第一个参数判断是否发送心跳还是探针消息，第二个是收到响应后的回调方法
    @SuppressWarnings("NonAtomicOperationOnVolatileField")
    private void sendEmptyEntries(final boolean isHeartbeat, final RpcResponseClosure<RpcRequests.AppendEntriesResponse> heartBeatClosure) {


         //开是构建AppendEntriesRequest请求，该请求是共用的，日志传输，心跳消息，探针消息都会使用这个请求封装消息内容
        final RpcRequests.AppendEntriesRequest.Builder rb = RpcRequests.AppendEntriesRequest.newBuilder();
        
        //填充请求中的公共属性
        if (!fillCommonFields(rb, this.nextIndex - 1, isHeartbeat)) {
            if (isHeartbeat && heartBeatClosure != null) {
                RpcUtils.runClosureInThread(heartBeatClosure, new Status(RaftError.EAGAIN,
                        "Fail to send heartbeat to peer %s", this.options.getPeerId()));
            }
            return;
        }
        try {
            //当前发送请求的时间
            final long monotonicSendTimeMs = Utils.monotonicMs();
            //根据方法中的参数判断是否为心跳请求
            if (isHeartbeat) {

                //省略了发送心跳消息的流程
                
            } else {
                
                //走到这里意味着不是心跳消息，而是探针消息
                rb.setData(ByteString.EMPTY);
                
                //创建AppendEntriesRequest对象封装探针消息
                final RpcRequests.AppendEntriesRequest request = rb.build();
                
                //发送探针消息
                final Future<Message> rpcFuture = this.rpcService.appendEntries(this.options.getPeerId().getEndpoint(),
                        request, -1, new RpcResponseClosureAdapter<RpcRequests.AppendEntriesResponse>() {

                            @Override
                            public void run(final Status status) {
                                //该方法会在接收到探针请求的响应后被回调
                                onRpcReturned(Replicator.this.id, status, request, getResponse(), monotonicSendTimeMs);
                            }
                        });

            }
            LOG.debug("Node {} send HeartbeatRequest to {} term {} lastCommittedIndex {}", this.options.getNode()
                    .getNodeId(), this.options.getPeerId(), this.options.getTerm(), rb.getCommittedIndex());
        } finally {
            unlockId();
        }
        
    }




     static void onRpcReturned(final ThreadId id, final Status status, final Message request, final Message response,final long rpcSendTime) {
        //对ThreadId判空
        if (id == null) {
            return;
        }
        Replicator r;
        //上锁，同时也对复制器对象进行了判空以及是否被销毁的判断处理
        if ((r = (Replicator) id.lock()) == null) {
            return;
        }
       //定义一个是否继续发送日志的标记
        boolean continueSendEntries = false;
        final boolean isLogDebugEnabled = LOG.isDebugEnabled();
        StringBuilder sb = null;
        if (isLogDebugEnabled) {
            sb = new StringBuilder("Replicator ")
                    .append(r)
                    .append(" is processing RPC responses, ");
        }
        try {//在这里开始处理探针请求响应和日志请求相应了，如果返回为true，说明上一次发送的请求没有问题
            //可以继续发送日志给跟随者了
            continueSendEntries = onAppendEntriesReturned(id,(RpcRequests.AppendEntriesRequest)request,(RpcRequests.AppendEntriesResponse)response,rpcSendTime, r);
        } finally {
            if (isLogDebugEnabled) {
                sb.append("after processed, continue to send entries: ")
                        .append(continueSendEntries);
                LOG.debug(sb.toString());
            }
            //判断是否可以继续发送日志给跟随者节点
            if (continueSendEntries) {
                //在这里继续发送下一批日志
                r.sendEntries();
            }
        }
    }




    //省略其他内容
}

```
在上面的代码块中，大家可以看到，**当领导者接收到探针请求的响应之后，就会回调 onRpcReturned 方法。而在 onRpcReturned 方法之中，通过 onAppendEntriesReturned 方法处理了探针请求的响应后，如果返回为 true，就意味着领导者可以向跟随者节点传输日志了，日志传输也就此开始了**。领导者处理探针请求的响应可以用下面代码块来表示。
```java
onRpcReturned ——> onAppendEntriesReturned ——> r.sendEntries()
```
这个流程非常清楚，也许大家会觉得我分析了半天好像是在说废话。那么，接下来我想请问大家，onRpcReturned 方法是怎么被回调的呢？之前我总是跟大家说当领导者接收到跟随者节点回复的响应之后，onRpcReturned 方法就会被回调。当然，实际上是 RpcResponseClosureAdapter 对象中的 run 方法被回调，然后再执行 onRpcReturned 方法。RpcResponseClosureAdapter 对象是在领导者向跟随者节点发送探针请求时创建的，就是下面代码块中展示的这样。
```java
//发送探针消息
inal Future<Message> rpcFuture = this.rpcService.appendEntries(this.options.getPeerId().getEndpoint(),
                    //在这里创建了RpcResponseClosureAdapter 对象                                      
       request, -1, new RpcResponseClosureAdapter<RpcRequests.AppendEntriesResponse>
           @Override
           public void run(final Status status) {
               //该方法会在接收到探针请求的响应后被回调
               onRpcReturned(Replicator.this.id, status, request, getResponse(), monotonicSendTimeMs);
           }
       });
```
可以看到，RpcResponseClosureAdapter 对象实际上是传进了 appendEntries 方法中。我当时跟大家说，**这个 RpcResponseClosureAdapter 对象就是专门为处理跟随者节点的响应而定义的，当领导者接收到响应后，就会回调 RpcResponseClosureAdapter 对象中的 run 方法，然后进一步执行 onRpcReturned 方法**。那现在问题又来了，这个 RpcResponseClosureAdapter 对象的 run 方法是怎么被回调的呢？这就要继续向 appendEntries 方法中查看了。我把 appendEntries 方法的代码展示在下面的代码块中了，请看下面代码块。
```java
public Future<Message> appendEntries(final Endpoint endpoint, final RpcRequests.AppendEntriesRequest request,
                                         final int timeoutMs, final RpcResponseClosure<RpcRequests.AppendEntriesResponse> done) {

    //在这里得到了领导者专门为Replicator复制器对象分配的单线程执行器
    final Executor executor = this.appendEntriesExecutorMap.computeIfAbsent(endpoint, k -> appendEntriesExecutors.next());

        
    if (!checkConnection(endpoint, true)) {
            return onConnectionFail(endpoint, request, done, executor);
        }

    //done就是刚才创建的RpcResponseClosureAdapter对象，executor就是刚刚得到的单线程执行器
    return invokeWithDone(endpoint, request, done, timeoutMs, executor);
}
```
现在程序执行到了 invokeWithDone 方法，所以我们又要看看这个方法的代码是怎么样的，请看下面代码块。
```java
//这个是RPC客户端发送请求的入口方法，从名字就能看出来是异步的，因为要通过回调函数来处理响应
public <T extends Message> Future<Message> invokeWithDone(final Endpoint endpoint, final Message request,
                                                          final InvokeContext ctx,
                                                          final RpcResponseClosure<T> done, final int timeoutMs,
                                                          final Executor rpcExecutor) {
    //得到客户端
    final RpcClient rc = this.rpcClient;
    //创建一个future对象，这个就可以当作Netty中的promise
    final FutureImpl<Message> future = new FutureImpl<>();
    //得到业务线程池，这个线程池的作用就是在当前节点收到响应后，执行一些回调方法
    //这里的这个业务线程池就是从appendEntries方法中得到的单线程执行器
    final Executor currExecutor = rpcExecutor != null ? rpcExecutor : this.rpcExecutor;
    
    try {
        
        //开始异步发送请求了，再往下调用，逻辑就会陷入bolt框架中了，这部分的逻辑讲解，我录制到视频中了
        //这里的异步指的是发送了请求不必同步等待响应，当响应到达时，直接执行回调方法即可
        //回调方法就是该方法中的第三个参数，创建了一个InvokeCallback对象，这个对象内部持有者上面得到的
        //currExecutor，所以，这个currExecutor就是用来执行回调方法的
        //InvokeCallback对象中要被回调的就是complete方法，具体回调的时机和逻辑都在视频中讲解
        rc.invokeAsync(endpoint, request, ctx, new InvokeCallback() {
            @SuppressWarnings({ "unchecked", "ConstantConditions" })
            @Override
            public void complete(final Object result, final Throwable err) {
                //发送请求的操作是否取消了
                if (future.isCancelled()) {
                    onCanceled(request, done);
                    return;
                }
                if (err == null) {
                    Status status = Status.OK();
                    Message msg = (T) result;
                    //这里的done就是RpcResponseClosureAdapter对象
                    if (done != null) {
                        try {
                            if (status.isOk()) {
                                done.setResponse((T) msg);
                            }//在这里执行了RpcResponseClosureAdapter对象的run方法
                            done.run(status);
                        } catch (final Throwable t) {
                          LOG.error("Fail to run RpcResponseClosure, the request is {}.", request, t);
                      }
                  }
              }
            } 
          
          
          //InvokeCallback对象内部持有着执行器
          //当该方法被调用的时候，就会获得为Replicator复制器对象分配的单线程执行器
          @Override
          public Executor executor() {
              return currExecutor;
          
          
      }, timeoutMs <= 0 ? this.rpcOptions.getRpcDefaultTimeout() : timeoutMs);
  } catch (final RemotingException e) 
      //省略部分代码
  
  return future;


}

```
可以看到，在上面的代码块中，**当程序执行 rc.invokeAsync 代码的时候，又创建了一个 InvokeCallback 对象，在这个对象中定义了两个方法，一个是 complete 方法，一个是 executor 方法，当 InvokeCallback 对象执行它的 complete 方法时，就会调用 RpcResponseClosureAdapter 对象的 run 方法。这时候大家应该也可以意识到了， rc.invokeAsync 就是异步把请求发送给跟随者节点的操作，在发送请求的时候，又定义了一个 InvokeCallback 回调对象，这个对象中的 complete 方法就是专门用来处理领导者接收到的响应的，显然 complete 方法会在领导者接收到响应的时候被回调。当 complete 方法被回调之后，就会在 complete 方法内部进一步执行 RpcResponseClosureAdapter 对象的 run 方法，在 run 方法中执行了 onRpcReturned 方法**。所以，现在让我再来为大家总结一下领导者处理跟随者节点响应的流程，就变成了下面这样。请看下面代码块。
```java
InvokeCallback.complete() ——> RpcResponseClosureAdapter.run() ——> onRpcReturned() ——> onAppendEntriesReturned() ——> r.sendEntries()
```
很好，如果大家理解了上面的那个逻辑，接下来就请听我另外两个问题，**InvokeCallback 对象的 complete 方法会在什么时候被回调呢？InvokeCallback 对象的 executor() 方法会在什么之后被调用呢**？因为当  executor() 方法被调用的时候，就会获得领导者为 Replicator 复制器对象分配的单线程执行器，这个单线程执行器肯定会发挥非常重要的作用，所以我对 executor() 方法什么时候被调用也很好奇。<br />但我们再往方法内部查看，程序的执行流程就会来到 bolt 框架中，这个框架的知识我在录制的视频中讲解了，所以下面我就直接为大家展示 InvokeCallback 对象的 complete() 方法和 executor() 方法被回调的时机。请看下面代码块。
```java
//这是一个监听器，当bolt框架将请求发送给跟随者节点时，会注册一个监听器对象，监听器对象中的
//onResponse方法会被接收到响应后被回调
public class RpcInvokeCallbackListener implements InvokeCallbackListener {

    private static final Logger logger = BoltLoggerFactory.getLogger("RpcRemoting");

    private String address;

    public RpcInvokeCallbackListener() {

    }

    public RpcInvokeCallbackListener(String address) {
        this.address = address;
    }


    //该方法会被接收到响应后被回调
    @Override
    public void onResponse(InvokeFuture future) {
        //这里就得到了之前创建的那个InvokeCallback对象
        InvokeCallback callback = future.getInvokeCallback();
        if (callback != null) {
            //创建一个任务
            CallbackTask task = new CallbackTask(this.getRemoteAddress(), future);
            //在这里执行了InvokeCallback对象的executor()方法，得到了领导者为
            //Replicator复制器对象专门分配的单线程执行器，并且对单线程执行器判空
            if (callback.executor() != null) {
                try {
                    //如果单线程执行器不为空，就把刚才创建的CallbackTask任务交给单线程执行器
                    //其实就是提交到单线程执行器的任务队列中
                    callback.executor().execute(task);
                } catch (RejectedExecutionException e) {
                    if (callback instanceof RejectionProcessableInvokeCallback) {
                        switch (((RejectionProcessableInvokeCallback) callback)
                            .rejectedExecutionPolicy()) {
                            case CALLER_RUNS:
                                task.run();
                                break;
                            case CALLER_HANDLE_EXCEPTION:
                                callback.onException(e);
                                break;
                            case DISCARD:
                            default:
                                logger.warn("Callback thread pool busy. discard the callback");
                                break;
                        }
                    } else {
                        logger.warn("Callback thread pool busy.");
                    }
                }
            } else {
                task.run();
            }
        }
    }



    class CallbackTask implements Runnable {

        InvokeFuture future;
        
        String remoteAddress;

        
        public CallbackTask(String remoteAddress, InvokeFuture future) {
            this.remoteAddress = remoteAddress;
            this.future = future;
        }


        @Override
        public void run() {

            //又一次得到了InvokeCallback对象
            InvokeCallback callback = future.getInvokeCallback();
            //这个response里面持有者跟随者节点回复的真正响应
            ResponseCommand response = (ResponseCommand) future.waitResponse(0);
            //把跟随者节点回复的响应传给InvokeCallback的complete方法
            //到此为止complete方法就被回调了
            callback.complete(rpcResponse.getResponseObject(),null);
            
        }
    }

    //省略部分内容


}
```
现在让我来为大家梳理一下上面代码块的执行流程，bolt 框架再把消息发送出去的时候会注册一个 RpcInvokeCallbackListener 监听器对象，这个监听器对象中的方法会在 bolt 框架定义的客户端，也就是领导者节点接收到来自跟随者节点的请求之后被回调，具体回调的方法就是 RpcInvokeCallbackListener 监听器对象中的 onResponse 方法。当这个方法回调的时候，bolt 框架中的线程会先执行 InvokeCallback 对象的 executor() 方法，得到领导者专门为 Replicator 复制器对象分配的单线程执行器，然后会把处理跟随者节点响应的操作封装成一个 task 任务对象提交给 Replicator 复制器对象的单线程执行器，并且这个 task 任务对象会被放到 Replicator 复制器对象的单线程执行器的任务队列中，等待被单线程执行器执行。当单线程执行器要执行这个 task 的时候，就会执行到 InvokeCallback 对象的 complete 方法，在 complete 方法中会一路调用，最终执行到 onRpcReturned 方法处理请求响应，如果这个响应是一个探针请求的响应，那就在判断了这个响应是成功响应之后，就可以开始给跟随者节点发送日志了。所以，现在我再来为大家总结一下领导者处理跟随者节点响应的全流程，就可以重构为下面这样。
```java
RpcInvokeCallbackListener.onResponse(future) ——> new CallbackTask() ——> callback.executor().execute(task) ——> 流程结束了
                                                                                                        //Replicator的单线程执行器执行
                                                                                                          ——> InvokeCallback.complete() ——> RpcResponseClosureAdapter.run() ——> onRpcReturned() ——> onAppendEntriesReturned() ——> r.sendEntries()

```
如果大家理解了上面的这个逻辑，接下来，就让我为大家描述一个场景，看看大家是否能理解：**当 raft 集群中的一个节点成为领导者之后，领导者就会向集群中的其他跟随者节点发送探针请求，假如领导者向集群中的某个节点发送了探针请求之后，接收到了探针请求的响应。处理响应的操作会被 bolt 框架封装成一个任务提交给跟随者节点对应的 Replicator 单线程执行器，也就是放到单线程执行器的任务队列中，然后单线程执行器去执行这个任务。经过一系列方法调用，领导者会执行到 onRpcReturned 方法中。如果在 onRpcReturned 方法中领导者发现跟随者回复的这个探针响应是成功的，就会直接确定下一条要发送给跟随者节点的日志索引。紧接着，领导者就会执行 onRpcReturned 方法中的 r.sendEntries() 这行代码，开始向跟随者节点传输日志，比如说领导者向跟随者节点传输了 3 批日志。也许在领导者传输第二批日志的时候，跟随者节点已经把第一批日志的响应回复给领导者。这个响应的处理操作会被 bolt 封装成一个任务放到单线程执行器的任务队列中，等到领导者发送完第三批日志，并且领导者内部没有日志可以发送了，这时候，领导者处理探针请求响应的操作才算执行完毕，也就是说 Replicator 复制器对象的单线程执行器执行完了任务队列中的第一个任务，紧接着，这个单线程执行器就可以去执行任务队列中的第二个任务，也就是处理发送的第一批日志的响应**。之后的每个响应都是按照这个流程处理的。大家可以仔细品味一下我描述的这个场景，自己梳理一下领导者处理跟随着节点响应的全流程，如果这个流程彻底理解了，那么接下来的内容就没什么难度了。

<a name="NMJn2"></a>
## 实现领导者处理日志请求响应的 Pipeline 模式

好了朋友们，现在让我们回归正题，看看在文章一开始就提出的三个问题：<br />**1 跟随者节点回复的响应怎么放到响应队列中？**<br />**2 回复的响应用不用定义什么相关的对象来封装，毕竟要给这个响应设置响应序号，那这个响应序号要怎么设置？**<br />**3** **当领导者接收到日志传输失败的响应后，应该只重复传输失败的这一批日志，还是从失败的日志开始，重新传输该批日志和之后所有的日志呢？**<br />首先请大家看第一个问题，跟随者节点回复给领导者的响应怎么放到响应队列中。在上一章我为 Pipeline 模式定义了基本的模式，引入了请求队列和响应队列。领导者每向跟随者节点发送一个请求，就会创建一个与请求对应的 Inflight 对象，把这个 Inflight 对象放到请求队列中。当领导者接收到跟随者节点回复的响应时，也应该把响应放到相应队列中，和请求队列中的每一个 Inflight 对象一一对应。那么领导者怎么把跟随者节点回复的响应存放到相应队列中了呢？单看这个问题非常简单，**因为领导者实际上是在 onRpcReturned 方法中才能真正获得跟随者节点回复的响应，所以肯定就是在 onRpcReturned 方法中把跟随者节点回复的响应存放到响应队列中**。就像下面代码块中展示的这样，请看下面代码块。
```java
//整个jraft框架中第二核心类，该类的对象就是一个复制器，如果当前节点是领导者，那么集群中的其他节点都会被领导者包装成一个个
//复制器对象，心跳检测，日志传输都是通过复制器对象进行的
public class Replicator implements ThreadId.OnError {

    //省略其他内容

    //响应队列，从跟随者收到的没一个响应都会先放到这个相应队列中
    //因为收到的每一个响应的序号可能并不能和要求的请求序号一一对应，所以总要有地方存放这些响应
    private final PriorityQueue pendingResponses = new PriorityQueue(50);




    //该方法会在探针请求收到响应后被回调，也会在给跟随者发送传输日志的请求，收到响应后被回调
    //参数中的response就是领导者接收到的响应
    static void onRpcReturned(final ThreadId id, final Status status, final Message request, final Message response,final long rpcSendTime) {


        //在该方法中把响应添加到响应队列中，具体怎么添加暂不实现
    }



    //省略其他内容
    
}
```
好了，上面代码块中的内容就把第一个问题解决了。但我刚才也说了，单看第一个问题确实很简单，但假如把第一个问题和第二个问题结合起来呢？那就是要不要再给领导者接收到的响应做一层封装，毕竟每一个响应也有对应的序号。与此同时，这个问题还引申出一个问题，那就是怎么给跟随者回复的响应设置响应序号？接下来让我来为大家分析一下。<br />我的观点是：**肯定要对跟随者回复给领导者的响应再做一下封装，因为领导者接收到的跟随者回复的响应，是遵循Protobuf 二进制协议定义出来的类型，响应体中根本没有响应序号这个字段。可我们现在非常需要一个和请求序号对应的响应序号，这样领导者才能知道自己处理的是哪个请求的响应，所以应该对收到的响应做一层封装。比如我们可以定义一个新的类型，就叫做 RpcResponse，用这个类型的对象来封装跟随者节点回复的响应**。在这个 RpcResponse 类型中定义两个成员变量，一个是跟随者节点回复的响应，一个就是响应序号。当领导者接收到跟随者节点的响应之后，就可以在 onRpcReturned 方法中创建一个 RpcResponse 对象来封装领导者接收到的响应，然后把 RpcResponse 对象存放到响应队列中。具体实现请看下面代码块。
```java
//整个jraft框架中第二核心类，该类的对象就是一个复制器，如果当前节点是领导者，那么集群中的其他节点都会被领导者包装成一个个
//复制器对象，心跳检测，日志传输都是通过复制器对象进行的
public class Replicator implements ThreadId.OnError {

    //省略其他内容

    //响应优先级队列，从跟随者收到的没一个响应都会先放到这个响应队列中
    //因为收到的每一个响应的序号可能并不能和要求的请求序号一一对应，所以总要有地方存放这些响应
    //泛型为RpcResponse类型
    private final PriorityQueue<RpcResponse> pendingResponses = new PriorityQueue<>(50);




    //该方法会在探针请求收到响应后被回调，也会在给跟随者发送传输日志的请求，收到响应后被回调
    //参数中的response就是领导者接收到的响应
    static void onRpcReturned(final ThreadId id, final Status status, final Message request, final Message response,final long rpcSendTime) {

        //这里得到的是当前复制器对象的存放跟随者响应的优先级队列
        final PriorityQueue<RpcResponse> holdingQueue = r.pendingResponses;

        //将接收到这这个响应放到响应队列中，这里看到，这个响应会被包装到RpcResponse对象中，然后把RpcResponse对象放到响应队列中
        //这个holdingQueue是一个优先级队列，存放RpcResponse对象的时候，是会按照响应序号排序的
        //但是响应序号是怎么获得的呢？这个seq究竟是从哪里传过来的？onRpcReturned方法的参数中并没有这个seq
        holdingQueue.add(new RpcResponse(seq, response));

        
    }




    //Replicator的一个内部类
    //从跟随者节点收到的响应会被包装到这个类的对象中，在包装的时候就会给每个响应分配好响应序号了
    static class RpcResponse implements Comparable<RpcResponse> {
       
        //从跟随者节点接收到的响应
        final Message response;
        
        //响应序号
        final int seq;

        //构造方法
        public RpcResponse(final int seq, final Message response) {
         
            this.seq = seq;
            this.response = response;
        }


        //将RpcResponse对象存放到pendingResponses队列时，会使用这方法进行对比
        @Override
        public int compareTo(final RpcResponse o) {
            return Integer.compare(this.seq, o.seq);
        }
    }



    //省略其他内容
    
}
```
在上面代码块中有一个问题，也是接下来让解决的一个问题，那就是跟随者节点回复给领导者的响应序号应该怎么被赋值呢？要解决这个问题，那我们就要从源头上思考一下，我们希望的是在 Pipeline 日志传输模式中，领导者发送的每一个请求都要和回复的每一个响应一一对应，一一对应的关键手段就是使用请求序号。给每个请求创建要给请求序号，给每一个响应赋予一个响应序号，只要这两个相等，那么就可以认为请求和响应是一一对应的。 而每一个请求的请求序号是在发送请求之前就定义好了，**如果能把这个请求序号直接交给响应，那么跟随者节点回复的响应的序号肯定就和请求一致了**。至于怎么交给跟随者节点的响应，这个就太简单了，**领导者是在 onRpcReturned 方法中才会接收到跟随者回复的响应，只要让领导者在发送日志请求的时候把请求序号传到 onRpcReturned 方法中，这样一来，当领导者接收到跟随者节点的响应时，就可以直接在 onRpcReturned 方法中获得该响应对应的序号，然后就可以在创建 RpcResponse 对象的时候给响应序号赋值了**。具体实现请看下面代码块。
```java
//整个jraft框架中第二核心类，该类的对象就是一个复制器，如果当前节点是领导者，那么集群中的其他节点都会被领导者包装成一个个
//复制器对象，心跳检测，日志传输都是通过复制器对象进行的
public class Replicator implements ThreadId.OnError {

    //省略其他内容

    //响应优先级队列，从跟随者收到的没一个响应都会先放到这个响应队列中
    //因为收到的每一个响应的序号可能并不能和要求的请求序号一一对应，所以总要有地方存放这些响应
    //泛型为RpcResponse类型
    private final PriorityQueue<RpcResponse> pendingResponses = new PriorityQueue<>(50);




    //批量发送日志给跟随者的方法，nextSendingIndex是下一条要发送的日志
    private boolean sendEntries(final long nextSendingIndex) {

        final RpcRequests.AppendEntriesRequest.Builder rb = RpcRequests.AppendEntriesRequest.newBuilder();
        
        //填充公共字段
        if (!fillCommonFields(rb, nextSendingIndex - 1, false)) {
            //填充失败就安装快照，但是这里把安装快照的代码省略了
            //这里也有一个安装快照的操作
            return false;
        }


        //省略部分代码


        //构建AppendEntriesRequest请求对象，接下来就要开始发送请求了
        final RpcRequests.AppendEntriesRequest request = rb.build();
        
        final long monotonicSendTimeMs = Utils.monotonicMs();
        
        //得到本次请求的请求序号
        final int seq = getAndIncrementReqSeq();

        
        Future<Message> rpcFuture = null;
        try {//发送请求给跟随者
            rpcFuture = this.rpcService.appendEntries(this.options.getPeerId().getEndpoint(), request, -1,
                    new RpcResponseClosureAdapter<RpcRequests.AppendEntriesResponse>() {

                        @Override
                        public void run(final Status status) {
                            RecycleUtil.recycle(recyclable);
                            //这个方法会在接收到响应后被回调，在这里可以看到，把请求序号seq传递到onRpcReturned方法中了
                            //onRpcReturned方法的其他参数大家可以暂时忽略
                            onRpcReturned(Replicator.this.id, RequestType.AppendEntries, status, request, getResponse(),
                                    seq, v, monotonicSendTimeMs);
                        }
                    });
        } catch (final Throwable t) {
            RecycleUtil.recycle(recyclable);
            ThrowUtil.throwException(t);
        }
        
        //把本次请求的信息添加到inflights队列中
        addInflight(RequestType.AppendEntries, nextSendingIndex, request.getEntriesCount(), request.getData().size(),
                seq, rpcFuture);
        
        return true;
        
    }


    


    //该方法会在探针请求收到响应后被回调，也会在给跟随者发送传输日志的请求，收到响应后被回调
    //参数中的response就是领导者接收到的响应，方法中的其他参数可以暂时忽略
    static void onRpcReturned(final ThreadId id, final RequestType reqType, final Status status, final Message request,
                              final Message response, final int seq, final int stateVersion, final long rpcSendTime) {

        //这里得到的是当前复制器对象的存放跟随者响应的优先级队列
        final PriorityQueue<RpcResponse> holdingQueue = r.pendingResponses;

        //将接收到这这个响应放到响应队列中，这时候大家就知道这个seq请求序号是怎么来的了吧？
        holdingQueue.add(new RpcResponse(seq, response));


        //其他内容暂时省略

        
    }




    //Replicator的一个内部类
    //从跟随者节点收到的响应会被包装到这个类的对象中，在包装的时候就会给每个响应分配好响应序号了
    static class RpcResponse implements Comparable<RpcResponse> {
       
        //从跟随者节点接收到的响应
        final Message response;
        
        //响应序号
        final int seq;

        //构造方法
        public RpcResponse(final int seq, final Message response) {
         
            this.seq = seq;
            this.response = response;
        }


        //将RpcResponse对象存放到pendingResponses队列时，会使用这方法进行对比
        @Override
        public int compareTo(final RpcResponse o) {
            return Integer.compare(this.seq, o.seq);
        }
    }



    //省略其他内容
}
```
朋友们，到此为止，刚才提出的三个问题的前两个问题就全都解决了。接下来就应该解决第三个问题了。但是在解决第三个问题之前，我们还有必要关注一下重构之后的 onRpcReturned 方法的流程。首先有一点其实应该明确，那就是之前领导者怎么处理跟随者节点的响应，引入 Pipeline 模式之后，也应该像以前那样处理。**在 onRpcReturned 方法之中，真正处理跟随者节点响应的方法是 onAppendEntriesReturned 方法。之前这个方法的流程是什么样的，引入 Pipeline 模式后 onAppendEntriesReturned 方法的大概流程也应该像以前一样。不可能在引入 Pipeline 模式之前，领导者处理跟随者节点的时候要判断响应中的任期、响应中的 LastLogIndex，又或者是更新 nextIndex 的值。而引入 Pipeline 模式之后，就不需要执行上述流程了，这显然是不可能的。所以我们的关注重点并不是 onAppendEntriesReturned 方法，而是程序开始执行 onRpcReturned 方法到执行 onAppendEntriesReturned 方法之间的流程**。而 onRpcReturned 方法到执行 onAppendEntriesReturned 方法之间的流程就是领导者根据 Pipeline 模式找出响应与请求的关系，然后再把正确的响应交给 onAppendEntriesReturned 方法处理即可。我知道这句话可能有点难以理解，接下来请让我给大家举一个具体的例子。<br />在我给出一个具体的例子之前，请大家先思考一个问题，我们费尽苦心为程序引入 Pipeline 模式，除了可以让领导者专心致志地给跟随者节点发送日志请求，传输日志，除此之外，对我们的程序还能有什么帮助呢？也许大家都没有意识到，所以请允许我提醒一下。**我们在实现 Pipeline 模式的时候定义了两个队列，一个是存放和日志传输请求相关的 Inflight 对象的队列，一个是用来存放跟随者节点响应的的队列。并且，这两个队列都具备按照优先级存放数据的功能**。请大家思考一下，**为什么非要使用优先级队列来存放和请求相关的 Inflight 对象和响应呢？反正请求和响应的序号已经可以对应上了，毕竟已经实现了把请求序号赋值给响应的功能**。其实深入思考一下就能意识到，**使用优先级队列最直接的一个好处就是领导者可以按照请求发送的顺序处理响应，也就是说，最早发送的请求，其响应也会最早被领导者处理。因为领导者向跟随者节点传输日志是一个非常重要的操作，传输的日志不能有遗漏，顺序不能错乱，所以必须严格按照请求发送的顺序来处理响应，这样就能及时知道传输日志的哪个环节出问题了。**这么说可能还是有点抽象，所以接下来我就要给大家举具体的例子了。<br />请大家想象一个场景，假如领导者向跟随者节点发送了三批日志，分三个请求发送，分别是请求 1、2、3。这三个请求对应三个响应。当请求 1 的响应被领导者接收到的时候，肯定就会放到响应队列的第一位，请求二被接收到的时候，肯定就会被放到响应队列的第二位。当领导者处理响应的时候，肯定是从响应队列中获得响应，然后处理。这样一来，肯定是先处理请求 1 对应的响应，然后再处理请求 2 对应的响应，最后处理请求 3 对应的响应。按照这种方式来处理请求有一个显而易见的好处，**那就是假如当领导者处理请求 1 的响应时，就发现响应失败了，也就是说请求 1 传输的日志没有复制成功。比如说日志有遗漏，或者存在冲突，这时候领导者就会知道，既然请求 1 失败了，那么接下来的两个请求肯定也都失败了。不可能请求 1 失败了，结果请求 2、3 回复了成功响应。这是不可能的，日志的复制是严格按照索引顺序进行的**。那接下来领导者应该怎么办呢？显然，**领导者就应该从请求 1 传输的日志索引开始，重新传输之后的所有日志**。这时候，之前提出来的第三个问题也就解决了，**当领导者接收到日志传输失败的响应后，应该从失败的日志开始，重新传输该批日志和之后所有的日志。**<br />当然，我相信讲到这里，大家应该也能意识到了，当领导者在处理某个跟随者节点回复的日志请求响应时，发现该响应是失败的，也就意味着日志没有赋值成功，可能出现了各种各样的原因，总之领导者要重新向跟随者节点传输失败的日志。**按照惯例应该先发送一个探针请求，探针请求响应成功之后，领导者就可以向跟随者传输日志了。当然，之前请求队列和响应队列显然就应该清空了，或者说重置，因为已经是一批新的请求和响应了**。这个流程算是一个简单的补充吧。<br />好了朋友们，通过刚才的一长串分析，我们不仅解决了文章开始提出的三个问题，还明确了一个观点，**那就是领导者必须要按照请求发送的顺序处理响应**，只有这样，当日志传输一旦出现问题，就能立刻找出出现问题的源头，然后重新传输日志即可。所以现在就又出现了一个新的问题，**那就是领导者怎么知道接下来要处理请求序号为几的响应呢**？比如说领导者向跟随者节点发送了一个请求序号为 1 的请求，然后又发送了一个请求序号为 2 的请求。这就意味着这个领导者在处理跟随者节点回复的响应时，肯定要先处理响应序号为 1 的响应。那领导者怎么知道自己处理的就是响应序号为 1 的响应呢？这个也简单，**只需要在 Replicator 复制器对象中再定义一个新的成员变量，比如说就定义为 requiredNextSeq，意思就是要求处理的下一个响应的序号。这时候领导者再发送请求，发送了请求 1 给跟随者节点之后，就可以给 requiredNextSeq 赋值为 1，意味着下一个要处理的响应序号为 1。只有处理完了序号为 1 的响应，就可以自增 requiredNextSeq，意味着下一个要处理的响应序号为 2。如果领导者当前处理的响应的序号并不为 1，领导者就可以把这个响应放到响应优先级队列中，等待序号为 1 的响应到来之后再处理即可**。这个逻辑也很简单，想必也是容易理解的。<br />到此为止，我相信从程序开始执行 onRpcReturned 方法到执行 onAppendEntriesReturned 方法之间的流程，我已经为大家梳理完毕了。接下来，就让我为大家最后做一个总结：**当领导者中的 Replicator 一旦开始执行它的 onRpcReturned 方法，就意味着领导者接收到了一个响应，但这个响应的序号还是未知的，这时候就要先在 onRpcReturned 方法中为这个响应创建一个 RpcResponse 对象来封装这个响应，RpcResponse 对象创建完成之后，就可以把该对象放到响应优先级队列中了。接下来的一个步骤非常关键，那就是获得响应优先级队列队首的响应，这个响应的序号一定是最小的，然后使用这个响应序号和 requiredNextSeq 做对比，如果这两个值相等，就说明领导者现在处理的这个响应正是应该处理的，然后把响应交给 onAppendEntriesReturned 方法去处理即可，处理完毕之后，让 requiredNextSeq 自增 1。如果不相等，那么领导者就不处理响应，等待序号正确的响应到达即可**。<br />如果上面这个流程大家都能理解，现在我还想针对上面这个流程补充一点点细节。假如说领导者接收到了某个跟随者节点的响应，回调了 Replicator 对象中的 onRpcReturned 方法。在 onRpcReturned 方法中发现领导者正在处理的响应的序号为 5，而领导者即将处理的下一个响应的序号也为 5，也就是 requiredNextSeq 的值为 5。并且当把这个响应序号为 5 的响应放到优先级队列中时，这个响应恰好放在了优先级队列的队首。这样一来，领导者就可以直接处理这个序号为 5 的响应了。我现在的问题是，假如响应队列中还有很多响应，比如还有序号为 6、7、8 等等很多响应，那么领导者在 onRpcReturned 方法中处理完序号为 5 的响应后，可以继续处理之后的响应吗？答案是显而易见的，领导者当然应该继续处理之后的响应，原因有两点：<br />**1 因为响应序号正好可以和 requiredNextSeq 对应上，就应该一个接一个地处理。**<br />**2 再说，领导者接收响应之后会把响应都存放到响应队列中，如果响应 6 比响应 5 先到达，响应 6 到达的时候肯定无法被处理，所以就在 onRpcReturned 方法中被放到了响应队列中。如果响应 5 到达的时候不把响应 6 一起处理了，这时候处理响应 6 的 onRpcReturned 方法已经被回调过了，响应 6 就没办法被领导者处理了。**<br />所以，经过上面的分析，实际上在 onRpcReturned 方法中还应该定义一个循环，在循环中不断地从响应队列中获得队首的响应，用队首响应的序号和 requiredNextSeq 做对比，直到这两个值不相等，说明一批响应已经被处理完了，等待新的响应序号和 requiredNextSeq 对应上的时候，再处理响应即可。<br />好了，啰里啰唆写了一大堆文字，恐怕大家早就看烦了。坦诚地说，我写这篇文章的思路可能没有那么连贯，可能常常忘记前面自己写了什么，这是因为我写这篇文章的时候发烧了，嗓子像刀割一样疼，但是我又睡不着，只好写写文章。不过请大家放心，我会在为 sofajraft 框架录制的视频中把遗漏的和要补充的知识都讲解了，流程图也会为大家准备好。接下来，就请大家看看代码吧，就看看重构之后的 onRpcReturned 方法。请看下面代码块。
```java
//整个jraft框架中第二核心类，该类的对象就是一个复制器，如果当前节点是领导者，那么集群中的其他节点都会被领导者包装成一个个
//复制器对象，心跳检测，日志传输都是通过复制器对象进行的
public class Replicator implements ThreadId.OnError {

    //省略其他内容


    //所有发给跟随者的请求的信息都会被包装到Inflight对象中，在还没有收到对应的响应前，请求会一直存放在这个队列中
    //请求的响应被处理了，才会将请求从队列中移除
    //每次添加Inflight对象，都会从队列尾部添加，这样序号越小的Inflight对象，就会放在队首了
    private final java.util.ArrayDeque<Inflight> inflights = new ArrayDeque<>();
    

    //响应优先级队列，从跟随者收到的没一个响应都会先放到这个响应队列中
    //因为收到的每一个响应的序号可能并不能和要求的请求序号一一对应，所以总要有地方存放这些响应
    //泛型为RpcResponse类型
    private final PriorityQueue<RpcResponse> pendingResponses = new PriorityQueue<>(50);


    //要求的响应序号，复制器每发送一条请求，都会给请求分配个序号，处理相应的时候，也会按照请求的序号进行处理
    //假如当前复制器向对应的跟随者发送了一条请求，这条请求的序号为1，不管收没收到响应，还会持续发送请求，请求序号会一直递增
    //但是，在接收到响应的时候，肯定是按照顺序处理的，如果不按顺序处理，那么在判断是否提交日志的时候就会乱成一团
    //而这个成员变量，就是记录的要处理的相应的序号，如果发送了序号为1的请求，就会把1赋值给这个成员变量
    //表示在处理下一个相应的时候，要求下一个响应的序号必须是1，这样才能使请求和响应一一对应，而日志处理也不会乱序
    private int requiredNextSeq = 0;


    //复制器对象给跟随者发送的每一条请求的序号，这个序号是随着请求递增的
    //并且，这个序列号十分重要，要使用Pipeline模式发送消息，就必须使用这个请求序号
    //这样一来，每一个请求和响应都会根据这个序号一一对应了，处理日志结果的时候也就不会乱序了
    private int reqSeq = 0;    


    //该方法会在探针请求收到响应后被回调，也会在给跟随者发送传输日志的请求，收到响应后被回调
    //参数中的response就是领导者接收到的响应，方法中的其他参数可以暂时忽略
    static void onRpcReturned(final ThreadId id, final RequestType reqType, final Status status, final Message request,
                              final Message response, final int seq, final int stateVersion, final long rpcSendTime) {

        //这里得到的是当前复制器对象的存放跟随者响应的优先级队列
        final PriorityQueue<RpcResponse> holdingQueue = r.pendingResponses;

        //将接收到这这个响应放到响应队列中，这时候大家就知道这个seq请求序号是怎么来的了吧？
        holdingQueue.add(new RpcResponse(seq, response));


        //定义一个是否继续发送日志的标记
        boolean continueSendEntries = false;

        
        try {//定义一个局部变量，记录本次循环处理的响应的个数
            int processed = 0;
            
            //开始遍历队列
            while (!holdingQueue.isEmpty()) {
                //从响应队列中获得序号最小的那个响应，holdingQueue是优先级队列，排在队首的响应肯定是序号最小的响应
                //注意，现在处理的响应很可能并不是本次收到的响应，收到的响应直接就放到相应队列中了
                //然后根据序号由小到大依次处理
                final RpcResponse queuedPipelinedResponse = holdingQueue.peek();
                
                //判断这个响应的序号是不是等于要求处理的这个请求序号，如果不相等，说明现在处理的响应不符合要求
                //也就不能处理，否则会造成日志处理混乱，注意啊，这里是第一次遇到不相等的时候，就会退出循环
                //但是之前循环了那么多次，也许每一次的响应序号和要求的请求序号都匹配上了
                //那就会直接处理这个响应，然后记录处理相应的个数
                if (queuedPipelinedResponse.seq != r.requiredNextSeq) {
                    //这里判断一下，如果在这次循环中成功处理了一些响应，就记录日志
                    if (processed > 0) {
                        if (isLogDebugEnabled) {
                            sb.append("has processed ").append(processed).append(" responses, ");
                        }
                        break;
                    } else {
                        //如果没有成功处理响应，一个响应也没有处理，那就不再继续发送日志
                        //注意，这里是一次响应也没有处理，也就是说在循环中没有一个响应能匹配requiredNextSeq
                        //那就不继续发送日志了，但只要成功处理过响应，哪怕只有一次，也不会走到这里，还是会继续发送日志的
                        continueSendEntries = false;
                        id.unlock();
                        return;
                    }
                }
                //从响应队列中，把匹配成功的响应从队列中删除，上面已经获得响应了
                holdingQueue.remove();
                
                //自增处理响应的次数
                processed++;
                
                //从请求队列中取出队首的请求，按理说，这时候请求和响应已经匹配上了
                //取出的时候，就会把请求从队列中移除了
                final Inflight inflight = r.pollInflight();
                
                //校验请求与响应的序号是否相等，如果不相等，就意味着请求和响应队列中的数据有问题
                //数据匹配混乱了，这时候要重置队列
                if (inflight.seq != queuedPipelinedResponse.seq) {
                    LOG.warn(
                            "Replicator {} response sequence out of order, expect {}, but it is {}, reset state to try again.",
                            r, inflight.seq, queuedPipelinedResponse.seq);
                    //重置队列
                    r.resetInflights();
                    continueSendEntries = false;
                    //这里的这个方法就是设置一个定时器，在超过了设定时间后，再让当前复制器向跟随者节点发送探针消息
                    //然后再判断是否可以继续发送日志
                    r.block(Utils.nowMs(), RaftError.EREQUEST.getNumber());
                    return;
                }

                
                try {//这里就是根据具体的请求类型，其实也就是响应类型来处理具体的响应了
                    switch (queuedPipelinedResponse.requestType) {
                        case AppendEntries://复制日志请求收到的响应就交给下面这个方法处理
                            continueSendEntries = onAppendEntriesReturned(id, inflight, queuedPipelinedResponse.status,
                                    (RpcRequests.AppendEntriesRequest) queuedPipelinedResponse.request,
                                    (RpcRequests.AppendEntriesResponse) queuedPipelinedResponse.response, rpcSendTime, startTimeMs, r);
                            break;
                    }
                } finally {
                    if (continueSendEntries) {
                        //经过上面的一系列判断和处理后，如果仍然可以继续发送日志，说明肯定序号最小的响应肯定被处理成功了
                        //这就意味着可以让要求处理的请求序号自增，然后进入下一轮循环判断
                        r.getAndIncrementRequiredNextSeq();
                    } else {
                        //走到这里说明序号最小的响应并没有处理成功，但是响应处理失败的时候，已经在onAppendEntriesReturned方法中解锁了
                        //所以这里直接退出即可
                        break;
                    }
                }
            }
        } finally {
            //判断是否可以继续发送日志给跟随者节点
            if (continueSendEntries) {
                //在这里继续发送下一批日志
                r.sendEntries();
            }
        }

        
    }



    //Replicator的一个内部类
    //从跟随者节点收到的响应会被包装到这个类的对象中，在包装的时候就会给每个响应分配好响应序号了
    static class RpcResponse implements Comparable<RpcResponse> {
       
        //从跟随者节点接收到的响应
        final Message response;
        
        //响应序号
        final int seq;

        //构造方法
        public RpcResponse(final int seq, final Message response) {
         
            this.seq = seq;
            this.response = response;
        }


        //将RpcResponse对象存放到pendingResponses队列时，会使用这方法进行对比
        @Override
        public int compareTo(final RpcResponse o) {
            return Integer.compare(this.seq, o.seq);
        }
    }



    //省略其他内容
}
```
到此为止，我就为大家把 onRpcReturned 方法重构完毕了。当然，需要重构的还有一个 onAppendEntriesReturned 方法，但如我之前所说，该方法的整体流程并不会改变多少，只不过是在发现响应失败后要执行重置请求和响应队列等等操作，具体实现大家可以从我提供的第六版本代码中查看。因为 Pipeline 模式的整体流程我都已经为大家分析完毕了，说句开玩笑的话，我相信已经启发了大家的思维，所以接下来就交给大家自己去掌握了。并且，我还可以告诉大家，当大家看到我提供的第六版本代码时，肯定会发现我提供的 Pipeline 模式的实现比文章中展示的还要复杂一些，比如 Pipeline 的版本号，对响应队列存放数据容量限制的判断等等，这些我在文章中都没有讲解。也许大家会觉得我讲得不全面，但我也有自己的一个理念，首先我先声明我决不是想偷工减料，如果我要偷工减料就不会为大家准备各种学习资料了，就不会为大家录制视频、画流程图了。我的主张一直都很明确，如果我把每个版本代码中的知识都剖析清楚，并且实现了，这就不是手写框架了，而是纯粹的源码剖析，这不是我想要的。我想达到的效果就是我启发大家的思维，把流程和关键细节为大家分析完毕，核心功能实现完整，然后大家就可以去我提供的代码中尽情学习了。文章中并没有提到的知识，但是大家自己掌握了，我想这也是一种成就感，当然，代码中注释非常详细，足够帮助大家掌握那些无关紧要的知识了。<br />最后我还要再补充一点，本章和上一章我只是为大家实现了领导者 Pipeline 模式传输日志的功能，并没有为大家实现跟随者节点使用 Pipeline 模式回复领导者响应的功能。这是因为跟随者节点 Pipeline 模式的实现和 bolt 框架多多少少有一些关系，还和跟随者节点回复给领导者的响应类型有关系，所以我也放在对应的视频中为大家讲解了。**如果大家想先从代码中看看跟随者节点 Pipeline 模式是怎么实现的，可以直接从 AppendEntriesRequestProcessor 这个类入手**。这个类的注释在第六版本代码中补全了，非常详细，感兴趣的朋友可以先看一看。<br />好了朋友们，这一章就到此结束了，Pipeline 模式也实现完毕了。接下来就要为大家实现状态机功能，诸位，我们下一章再见！
