package com.nechet.server.connectionLogic;

import com.nechet.common.util.requestLogic.CommandDescription;
import com.nechet.common.util.requestLogic.RequestArgumentType;
import com.nechet.common.util.requestLogic.Requests.AnswerRequests;
import com.nechet.server.commandLogic.ServerCommandManager;
import com.nechet.server.commandLogic.comands.SaveCommand;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.util.Iterator;

public class TCPserver {
    private static final int BUFFER_SIZE = 4096;
    private ByteBuffer buffer;

    private static final String HOST = "localhost";
    private static final int PORT = 5070;

    private ServerSocketChannel serverSocketChannel;
    private Selector selector;
    private RequestHandler requestHandler;

    private Serializer<String, CommandDescription> serializer = new Serializer<>();

    public TCPserver(RequestHandler requestHandler) {
        this.requestHandler = requestHandler;
        this.buffer = ByteBuffer.allocate(BUFFER_SIZE);

    }
    public void openConnection() throws IOException {
        this.serverSocketChannel = ServerSocketChannel.open();
        this.serverSocketChannel.configureBlocking(false);
        InetSocketAddress inetSocketAddress = new InetSocketAddress(HOST, PORT);
        System.out.println(inetSocketAddress.getAddress());
        this.serverSocketChannel.bind(inetSocketAddress);
        this.selector = initSelector();
    }
    public void run() {
        try {
            while (true) {
                selector.selectNow();
                Iterator<SelectionKey> selectedKeys = selector.selectedKeys().iterator();
                while (selectedKeys.hasNext()) {
                    SelectionKey key = takeKey(selectedKeys);
                    handleKey(key);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    private SelectionKey takeKey(Iterator<SelectionKey> selectionKeyIterator) {
        SelectionKey key = selectionKeyIterator.next();
        selectionKeyIterator.remove();
        return key;
    }
    private Selector initSelector() throws IOException {
        Selector socketSelector = SelectorProvider.provider().openSelector();
        this.serverSocketChannel.register(socketSelector, SelectionKey.OP_ACCEPT);
        return socketSelector;
    }
    private void handleKey(SelectionKey key) throws IOException {
        if (key.isValid()) {
            if (key.isAcceptable()) {
                accept(key);
            } else if (key.isReadable()) {
                read(key);
            } else if (key.isWritable()) {
                write(key);
            } else {
                SaveCommand saveCommand = new SaveCommand();
                CommandDescription description = new CommandDescription("save", RequestArgumentType.NO_ARGS);
                saveCommand.execute(description);
            }
        }
    }
    private void accept(SelectionKey key) throws IOException {
        ServerSocketChannel ssc = (ServerSocketChannel) key.channel();
        SocketChannel socketChannel = ssc.accept();
        socketChannel.configureBlocking(false);
        //System.out.println("Подключенно: " + socketChannel.getRemoteAddress());
        socketChannel.register(selector, SelectionKey.OP_READ);
    }
    private void read(SelectionKey key) throws IOException {
        SocketChannel socketChannel = (SocketChannel) key.channel();
        this.buffer.clear();
        int bytesRead;
        try {
            bytesRead = socketChannel.read(this.buffer);
        } catch (IOException e) {
            key.cancel();
            socketChannel.close();
            return;
        }

        if (bytesRead == -1) {
            key.cancel();
            return;
        }
        this.buffer.flip();

        AnswerRequests response = requestHandler.handleRequest(buffer);
        //System.out.println(response);
        socketChannel.register(this.selector, SelectionKey.OP_WRITE, response);
    }
    public void close() throws IOException {
        SaveCommand saveCommand = new SaveCommand();
        CommandDescription description = new CommandDescription("save", RequestArgumentType.NO_ARGS);
        saveCommand.execute(description);
        if (serverSocketChannel != null) {
            serverSocketChannel.close();
        }
    }

    private void write(SelectionKey key) throws IOException {
        SocketChannel socketChannel = (SocketChannel) key.channel();
        AnswerRequests response = (AnswerRequests) key.attachment();
        ByteBuffer writeBuffer = serializer.serializeObject(response);
        writeBuffer.flip();
        while (writeBuffer.hasRemaining()) {
            socketChannel.write(writeBuffer);
        }

        socketChannel.register(selector, SelectionKey.OP_READ);
    }
}
