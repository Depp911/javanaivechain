package com.jayne.javanaivechain;

import java.util.ArrayList;
import java.util.List;

/**
 * 区块链
 *
 * Created by jayne on 2018/3/27.
 */
public class BlockService {
    private List<Block> blockChain;

    /**
     * 构造器
     */
    public BlockService() {
        //初始区块链
        this.blockChain = new ArrayList<Block>();
        //添加创世区块
        blockChain.add(this.getFristBlock());
    }

    /**
     * 获取最新的区块
     * @return
     */
    public Block getLatestBlock() {
        return blockChain.get(blockChain.size() - 1);
    }

    /**
     * 生成新的区块（挖矿）
     * @param blockData
     * @return
     */
    public Block generateNextBlock(String blockData) {
        Block previousBlock = this.getLatestBlock();
        int nextIndex = previousBlock.getIndex() + 1;
        long nextTimestamp = System.currentTimeMillis();
        String nextHash = calculateHash(nextIndex, previousBlock.getHash(), nextTimestamp, blockData);
        return new Block(nextIndex, previousBlock.getHash(), nextTimestamp, blockData, nextHash);
    }

    /**
     * 区块链同步
     * @param newBlock
     */
    public void addBlock(Block newBlock) {
        if (isValidNewBlock(newBlock, getLatestBlock())) {
            blockChain.add(newBlock);
        }
    }

    /**
     * 区块链分叉
     * @param newBlocks
     */
    public void replaceChain(List<Block> newBlocks) {
        if (isValidBlocks(newBlocks) && newBlocks.size() > blockChain.size()) {
            blockChain = newBlocks;
        } else {
            System.out.println("Received blockchain invalid");
        }
    }

    /**
     * 获取完整区块链
     * @return
     */
    public List<Block> getBlockChain() {
        return blockChain;
    }

    /**
     * 为了保存完整的数据，必须哈希区块。SHA-256会对块的内容进行加密
     * @param index
     * @param previousHash
     * @param timestamp
     * @param data
     * @return
     */
    private String calculateHash(int index, String previousHash, long timestamp, String data) {
        StringBuilder builder = new StringBuilder(index);
        builder.append(previousHash).append(timestamp).append(data);
        return CryptoUtil.getSHA256(builder.toString());
    }

    /**
     * 生成创世区块
     * @return
     */
    private Block getFristBlock() {
        return new Block(0, "0", System.currentTimeMillis(), "Hello Block", "aa212344fc10ea0a2cb885078fa9bc2354e55efc81be8f56b66e4a837157662e");
    }

    /**
     * 验证新的区块是否合法
     * @param newBlock
     * @param previousBlock
     * @return
     */
    private boolean isValidNewBlock(Block newBlock, Block previousBlock) {
        if (previousBlock.getIndex() + 1 != newBlock.getIndex()) {
            System.out.println("invalid index");
            return false;
        } else if (!previousBlock.getHash().equals(newBlock.getPreviousHash())) {
            System.out.println("invalid previoushash");
            return false;
        } else {
            String hash = calculateHash(newBlock.getIndex(), newBlock.getPreviousHash(), newBlock.getTimestamp(),
                    newBlock.getData());
            if (!hash.equals(newBlock.getHash())) {
                System.out.println("invalid hash: " + hash + " " + newBlock.getHash());
                return false;
            }
        }
        return true;
    }

    /**
     * 验证区块链是否合法
     * @param newBlocks
     * @return
     */
    private boolean isValidBlocks(List<Block> newBlocks) {
        Block fristBlock = newBlocks.get(0);
        //多节点需重写equals
        if (fristBlock.equals(getFristBlock())) {
            return false;
        }

        for (int i = 1; i < newBlocks.size(); i++) {
            if (isValidNewBlock(newBlocks.get(i), fristBlock)) {
                fristBlock = newBlocks.get(i);
            } else {
                return false;
            }
        }
        return true;
    }
}
