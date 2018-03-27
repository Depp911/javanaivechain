package com.jayne.javanaivechain;

import com.alibaba.fastjson.JSON;
import org.java_websocket.WebSocket;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.handshake.ServerHandshake;
import org.java_websocket.server.WebSocketServer;

import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * 节点
 * 节点的本质是和其他节点共享和同步区块链
 *
 * Created by jayne on 2018/3/27.
 */
public class P2PService {
    private List<WebSocket> sockets;
    private BlockService    blockService;
    //消息类型
    private final static int QUERY_LATEST        = 0;
    private final static int QUERY_ALL           = 1;
    private final static int RESPONSE_BLOCKCHAIN = 2;

    /**
     * 构造器
     * @param blockService
     */
    public P2PService(BlockService blockService) {
        this.blockService = blockService;
        this.sockets = new ArrayList<WebSocket>();
    }

    public void initP2PServer(int port) {
        final WebSocketServer socket = new WebSocketServer(new InetSocketAddress(port)) {
            //连接建立时触发
            @Override
            public void onOpen(WebSocket webSocket, ClientHandshake clientHandshake) {
                //发送获取最新区块的请求
                write(webSocket, queryChainLengthMsg());
                sockets.add(webSocket);
            }

            //连接关闭时触发
            @Override
            public void onClose(WebSocket webSocket, int i, String s, boolean b) {
                System.out.println("connection failed to peer:" + webSocket.getRemoteSocketAddress());
                sockets.remove(webSocket);
            }

            //接收数据时触发
            @Override
            public void onMessage(WebSocket webSocket, String s) {
                //处理请求
                handleMessage(webSocket, s);
            }

            //通信发生错误时触发
            @Override
            public void onError(WebSocket webSocket, Exception e) {
                System.out.println("connection failed to peer:" + webSocket.getRemoteSocketAddress());
                sockets.remove(webSocket);
            }

            @Override
            public void onStart() {

            }
        };
        socket.start();
        System.out.println("listening websocket p2p port on: " + port);
    }

    /**
     * 处理请求
     * @param webSocket
     * @param s
     */
    private void handleMessage(WebSocket webSocket, String s) {
        try {
            Message message = JSON.parseObject(s, Message.class);
            System.out.println("Received message" + JSON.toJSONString(message));
            switch (message.getType()) {
                case QUERY_LATEST:
                    //回复最新区块的请求
                    write(webSocket, responseLatestMsg());
                    break;
                case QUERY_ALL:
                    //回复完整区块链的请求
                    write(webSocket, responseChainMsg());
                    break;
                case RESPONSE_BLOCKCHAIN:
                    //处理应答请求
                    handleBlockChainResponse(message.getData());
                    break;
            }
        } catch (Exception e) {
            System.out.println("hanle message is error:" + e.getMessage());
        }
    }

    /**
     * 处理应答请求
     * @param message
     */
    private void handleBlockChainResponse(String message) {
        //获取应答区块链，并排序。排序的意义是什么？？
        List<Block> receiveBlocks = JSON.parseArray(message, Block.class);
        Collections.sort(receiveBlocks, new Comparator<Block>() {
            public int compare(Block o1, Block o2) {
                return o1.getIndex() - o1.getIndex();
            }
        });

        //获取应答区块链最新的区块
        Block latestBlockReceived = receiveBlocks.get(receiveBlocks.size() - 1);
        //获取本地区块链最新的区块
        Block latestBlock = blockService.getLatestBlock();
        if (latestBlockReceived.getIndex() > latestBlock.getIndex()) {
            //应答区块链长度大于本地区块链
            if (latestBlock.getHash().equals(latestBlockReceived.getPreviousHash())) {
                //应该区块链刚好比本地区块链长度大1，且符合新增区块规则，则同步最新区块
                System.out.println("We can append the received block to our chain");
                blockService.addBlock(latestBlockReceived);
                //广播新增了区块
                broatcast(responseLatestMsg());
            } else if (receiveBlocks.size() == 1) {
                System.out.println("We have to query the chain from our peer");
                //新增了区块，但与本地不吻合，此时向其他节点获取完整区块链
                broatcast(queryAllMsg());
            } else {
                //区块链分叉。简易逻辑？？？
                blockService.replaceChain(receiveBlocks);
            }
        } else {
            //应答区块链长度小于本地区块链，忽略（等于的情况，区块链分叉也忽略了）
            System.out.println("received blockchain is not longer than local blockchain. Do nothing");
        }
    }

    //连接其他节点
    public void connectToPeer(String peer) {
        try {
            final WebSocketClient socket = new WebSocketClient(new URI(peer)) {
                @Override
                public void onOpen(ServerHandshake serverHandshake) {
                    write(this, queryChainLengthMsg());
                    sockets.add(this);
                }

                @Override
                public void onMessage(String s) {
                    handleMessage(this, s);
                }

                @Override
                public void onClose(int i, String s, boolean b) {
                    System.out.println("connection failed");
                    sockets.remove(this);
                }

                @Override
                public void onError(Exception e) {
                    System.out.println("connection failed");
                    sockets.remove(this);
                }
            };
            socket.connect();
        } catch (URISyntaxException e) {
            System.out.println("p2p connect is error:" + e.getMessage());
        }
    }

    /**
     * 发送信息(单发)
     * @param ws
     * @param message
     */
    private void write(WebSocket ws, String message) {
        ws.send(message);
    }

    /**
     * 广播（群发）
     * @param message
     */
    public void broatcast(String message) {
        for (WebSocket socket : sockets) {
            this.write(socket, message);
        }
    }

    /**
     * 请求获取完整区块链
     * @return
     */
    private String queryAllMsg() {
        return JSON.toJSONString(new Message(QUERY_ALL));
    }

    /**
     * 请求获取最新区块
     * @return
     */
    private String queryChainLengthMsg() {
        return JSON.toJSONString(new Message(QUERY_LATEST));
    }

    /**
     * 响应获取完整区块链
     * @return
     */
    private String responseChainMsg() {
        return JSON.toJSONString(new Message(RESPONSE_BLOCKCHAIN, JSON.toJSONString(blockService.getBlockChain())));
    }

    /**
     * 响应获取最新区块
     * @return
     */
    public String responseLatestMsg() {
        Block[] blocks = {blockService.getLatestBlock()};
        return JSON.toJSONString(new Message(RESPONSE_BLOCKCHAIN, JSON.toJSONString(blocks)));
    }

    /**
     * 获取所有节点
     * @return
     */
    public List<WebSocket> getSockets() {
        return sockets;
    }
}
