//package com.gfm.io;
//
//import java.io.IOException;
//import java.net.InetSocketAddress;
//import java.nio.ByteBuffer;
//import java.nio.channels.AsynchronousServerSocketChannel;
//import java.nio.channels.AsynchronousSocketChannel;
//import java.nio.channels.CompletionHandler;
//import java.util.concurrent.CountDownLatch;
//
//public class AioEchoServer {
//    public static void main(String[] args) throws IOException {
//        int port = 9999;
//        System.out.println("Listening for connections on port :"+port);
//        final AsynchronousServerSocketChannel serverChannel = AsynchronousServerSocketChannel.open();
//        InetSocketAddress address = new InetSocketAddress(port);
//        //将ServerSocket绑定到指定的端口里
//        serverChannel.bind(address);
//        final CountDownLatch latch = new CountDownLatch(1);
//        serverChannel.accept(null, new CompletionHandler<AsynchronousSocketChannel, Object>() {
//            @Override
//            public void completed(final AsynchronousSocketChannel result, Object attachment) {
//                //一旦完成处理，再次接收新的客户端请求
//                serverChannel.accept(null,this);
//                ByteBuffer buffer = ByteBuffer.allocate(100);
//                //在channel里植入一个读操作EchoCompletionHandler,一旦buffer有数据写入，
//                result.read(buffer,buffer,new EchoCompletionHandler(result));
//            }
//
//            @Override
//            public void failed(Throwable exc, Object attachment) {
//                try {
//                    serverChannel.close();
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }finally {
//                    latch.countDown();
//                }
//            }
//        });
//    }
//
//    private static final class EchoCompletionHandler implements CompletionHandler<Integer,ByteBuffer> {
//
//        private AsynchronousSocketChannel channel;
//
//        public EchoCompletionHandler(AsynchronousSocketChannel channel){
//            this.channel = channel;
//        }
//
//        @Override
//        public void completed(Integer result, ByteBuffer attachment) {
//            attachment.flip();
//            //在channel里植入一个读操作CompletionHandler, 一旦channel有数据写入，CompletionHandler便会被唤醒
//            channel.write(attachment, attachment, new CompletionHandler<Integer, ByteBuffer>() {
//                @Override
//                public void completed(Integer result, ByteBuffer attachment) {
//                    if(attachment.hasRemaining()){
//                        //如果buffer里还有内容，则再次出发写入操作将buffer里的内容写入channel
//                        channel.write(attachment,attachment,this);
//                    }else{
//                        attachment.compact();
//                        //如果channel里还有内容需要读入到buffer里，则再次触发写入操作将channel里的内容读入buffer
//                        channel.read(attachment,attachment,EchoCompletionHandler.this);
//                    }
//                }
//
//                @Override
//                public void failed(Throwable exc, ByteBuffer attachment) {
//                    try {
//                        channel.close();
//                    } catch (IOException e) {
//                        e.printStackTrace();
//                    }
//                }
//            });
//        }
//
//        @Override
//        public void failed(Throwable exc, ByteBuffer attachment) {
//            try {
//                channel.close();
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        }
//    }
//}
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
