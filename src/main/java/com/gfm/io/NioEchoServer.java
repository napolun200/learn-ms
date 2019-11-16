package com.gfm.io;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

public class NioEchoServer {

    public static void main(String[] args) throws IOException {
        int port = 9999;
        System.out.println("listening for connection on port:"+port);
        ServerSocketChannel serverChannel = ServerSocketChannel.open();
        ServerSocket serverSocket = serverChannel.socket();
        InetSocketAddress address = new InetSocketAddress(port);

        //将ServerSocket绑定到指定的端口
        serverSocket.bind(address);
        serverChannel.configureBlocking(false);
        Selector selector = Selector.open();
        //将channel注册到Selector里，并说明让Selector关注的点，这里是关注建立连接这个事件
        serverChannel.register(selector, SelectionKey.OP_ACCEPT);
        while (true){
            try{
                //阻塞等待就绪的Channel, 没有与客户端建立连接前就一直查询
                selector.select();
            }catch (IOException ex){
                ex.printStackTrace();
                //结合业务，处理异常逻辑
                break;
            }

            //获取到Selector里所有就绪的SelectedKey实例，每将一个Channel注册到Selector就会产生一个SelectedKey实例
            Set<SelectionKey> readyKeys = selector.selectedKeys();
            Iterator<SelectionKey> iterator = readyKeys.iterator();
            while (iterator.hasNext()){
                SelectionKey key = iterator.next();
                //将就绪的SelectedKey从Selector中移除，因为马上就要处理它，防止重复执行
                iterator.remove();
                try{
                    if(key.isAcceptable()){
                        ServerSocketChannel server = (ServerSocketChannel) key.channel();
                        //接受客户端连接
                        SocketChannel client = server.accept();
                        System.out.println("Accepted connection from: "+client);
                        client.configureBlocking(false);
                        //向selector注册socketChannel,主要关注读写，并传入一个ByteBuffer实例供读写缓存
                        client.register(selector,SelectionKey.OP_WRITE | SelectionKey.OP_READ, ByteBuffer.allocate(100));
                    }
                    //若SelectedKey处于可读状态
                    if (key.isReadable()) {
                        SocketChannel client = (SocketChannel) key.channel();
                        ByteBuffer output = (ByteBuffer) key.attachment();
                        //从channel里读取数据存入到ByteBuffer
                        client.read(output);
                    }
                    //若SelectedKey处于可写状态
                    if(key.isWritable()){
                        SocketChannel client = (SocketChannel) key.channel();
                        ByteBuffer output = (ByteBuffer) key.attachment();
                        output.flip();
                        //将ByteBuffer里的数据写入到channel里
                        client.write(output);
                        output.compact(); //将已经完成的数据从缓存中移除
                    }
                }catch (Exception e){
                    key.cancel();
                    key.channel().close();
                }
            }
        }
    }

}



















