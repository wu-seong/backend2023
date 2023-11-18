package org.example;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;

import com.google.protobuf.*;
import mju.MessagePb;
import org.json.JSONArray;
import org.json.JSONObject;

import static org.example.MessageUtility.getMessage;
import static org.example.MessageUtility.getMessageSize;
import static org.example.ServerMode.JSON;
import static org.example.ServerMode.PROTOBUF;

public class Server {
    public static ServerMode mode = PROTOBUF; // 1 : JSON  2: PROTOBUF(DEFAULT)
    static final MessageHandler messageHandler = new MessageHandler();
    static HashMap<SocketChannel, User> clients = new HashMap<SocketChannel, User>(); //연결되어 관리될 클라이언트들
    private static final RoomManager roomManager = new RoomManager();
    private static final ProtobufCreator protobufCreator = new ProtobufCreator();
    private static ServerSocketChannel serverSocketChannel; //TCP 서버 소켓 채널 선언 (연결을 받을 소켓)
    private static ThreadPool threadPool;

    public static void main(String[] args) throws Exception {
        int numOfConsumerWorkers = Integer.parseInt(args[0]); //지정된 Consumer Workers 수
        setServerMode(Integer.parseInt(args[1]));
        Selector selector = Selector.open();
        threadPool = new ThreadPool(numOfConsumerWorkers, 10);

        InetSocketAddress inetSocketAddress = new InetSocketAddress(19128); //바인딩 정보 (포트만 주면 ip는 커널에서 임의로)

        serverSocketChannel = ServerSocketChannel.open(); //서버 소켓 생성, ServerSocketChannel의 구현클래스를 provider가 제공
        serverSocketChannel.socket().setReuseAddress(true);
        serverSocketChannel.bind(inetSocketAddress, 10);    //서버 소켓 바인딩
        serverSocketChannel.configureBlocking(false); //non-blocking

        serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT); //selector에 연결 이벤트 감지하도록 등록

        while (true) {
            selector.select();//이벤트가 들어올 때 까지 blocking;
            Set<SelectionKey> selectionKeys = selector.selectedKeys();
            Iterator<SelectionKey> iterator = selectionKeys.iterator();

            while (iterator.hasNext()) {
                SelectionKey key = iterator.next();
                if (key.isAcceptable()) { //서버 소켓에서 연결이 들어옴
                    SocketChannel clientSocket = serverSocketChannel.accept(); //listen + accepted (blocking)
                    connectProcess(clientSocket, selector);
                } else if (key.isReadable()) { //클라이언트 소켓에서 데이터를 보냄 or 연결 끊김
                    SocketChannel client = (SocketChannel) key.channel();
                    switch (mode) {
                        case JSON:
                            ByteBuffer lengthBuffer = ByteBuffer.allocate(2); //형식적인 헤더 읽기
                            int read = client.read(lengthBuffer);   //길이 > 0이면 연결종료 FIN메시지
                            if (read <= 0) {
                                clients.remove(client);
                                key.cancel();
                                client.close();
                                iterator.remove();
                                continue;
                            }
                            JSONObject jsonObject = parseJSON(client);
                            JSONObjectWrapper jsonObjectWrapper = new JSONObjectWrapper(jsonObject, client);
                            threadPool.execute(jsonObjectWrapper);
                            break;
                        case PROTOBUF:
                            short messageSize = getMessageSize(client);
                            if (messageSize <= 0) { //연결 종료 감지, FIN이면 read가 0을 반환
                                clients.remove(client);
                                key.cancel();
                                client.close();
                                iterator.remove();
                                continue;
                            } else if (messageSize == 2) {      //메세지 두개 한번에 읽고 taskQueue로 올리기 (Protobuf mode), Type이 먼저들어와야 처리
                                byte[] typeMessage;
                                byte[] requestMessage;
                                BinaryMessage binaryMessage = new BinaryMessage();
                                binaryMessage.setSocketChannel(client);
                                typeMessage = getMessage(client, messageSize);
                                MessagePb.Type type;
                                try {
                                    type = MessagePb.Type.parseFrom(typeMessage);
                                }
                                catch (InvalidProtocolBufferException e){  //만약 타입메시지를 먼저 읽어오는 것이 아니라 2바이트의 다른 요청이 먼저 오면 잘못된 요청이므로 무시
                                    continue;
                                }
                                MessagePb.Type.MessageType messageType = type.getType();
                                int number = messageType.getNumber();
                                while (!binaryMessage.isSet()) {   //가끔씩 타입만 먼저 와서 읽는 경우가 있어서 그럴 때 다시 읽도록 반복
                                    messageSize = getMessageSize(client);
                                    requestMessage = getMessage(client, messageSize);
                                    binaryMessage.setBinary(typeMessage, requestMessage);
                                    if (number == 1 || number == 4 || number == 6) { //request body가 원래 없는 것들은 다시 일기 않도록
                                        break;
                                    }
                                }
                                threadPool.execute(binaryMessage);
                            }
                            break;

                    }
                }
                iterator.remove();
            }
        }
    }

    public static JSONObject parseJSON(SocketChannel client) {

        ByteBuffer buffer = ByteBuffer.allocate(1024); //받을 수 있는 JSON 최대 크기
        StringBuilder jsonStrBuilder = new StringBuilder();
        while (true) {
            buffer.clear();
            int read = 0;
            try {
                read = client.read(buffer);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            if (read == -1) {
                break;
            }
            buffer.flip();
            jsonStrBuilder.append(StandardCharsets.UTF_8.decode(buffer));
            if (!buffer.hasRemaining()) {
                break;
            }
        }
        String jsonStr = jsonStrBuilder.toString();
        System.out.println("jsonStr = " + jsonStr);
        JSONObject jsonObject = new JSONObject(jsonStr);
        return jsonObject;
    }

    public static void connectProcess(SocketChannel clientSocket, Selector selector) throws IOException {
        User user = new User();  //유저에 소켓 관련정보 등록
        user.setSocketChannel(clientSocket);
        SocketAddress localAddress = clientSocket.getRemoteAddress();
        user.setName(localAddress.toString());

        clients.put(clientSocket, user); //연결 완료 시 관리 클라이언트에 추가
        clientSocket.configureBlocking(false);
        clientSocket.register(selector, SelectionKey.OP_READ);
        System.out.println("clientSocket  = " + clientSocket + "연결");
    }

    static void setServerMode(int modeNumber) {
        if (modeNumber == 1) {
            mode = JSON;
        } else if (modeNumber == 2) {
            mode = PROTOBUF;
        } else {
            System.out.println("잘못된 모드 넘버");
            System.exit(0);
        }
    }

    static class MessageHandler {
        private final Map<MessagePb.Type.MessageType, Consumer<ProtoMessageWrapper>> protobufHandlers = new HashMap<>();
        private final Map<String, Consumer<JSONMessageWrapper>> JSONHandlers = new HashMap<>();

        public MessageHandler() {
            protobufHandlers.put(MessagePb.Type.MessageType.CS_NAME, this::nameType);
            protobufHandlers.put(MessagePb.Type.MessageType.CS_ROOMS, this::roomsType);
            protobufHandlers.put(MessagePb.Type.MessageType.CS_CREATE_ROOM, this::createRoomType);
            protobufHandlers.put(MessagePb.Type.MessageType.CS_JOIN_ROOM, this::joinRoom);
            protobufHandlers.put(MessagePb.Type.MessageType.CS_LEAVE_ROOM, this::leaveRoom);
            protobufHandlers.put(MessagePb.Type.MessageType.CS_CHAT, this::CSChatType);
            protobufHandlers.put(MessagePb.Type.MessageType.CS_SHUTDOWN, this::shutdownType);
            JSONHandlers.put("CSName", this::nameType);
            JSONHandlers.put("CSCreateRoom", this::createRoomType);
            JSONHandlers.put("CSRooms", this::roomsType);
            JSONHandlers.put("CSJoinRoom", this::joinRoom);
            JSONHandlers.put("CSLeaveRoom", this::leaveRoom);
            JSONHandlers.put("CSChat", this::CSChatType);
            JSONHandlers.put("CSShutdown", this::shutdownType);
        }

        public void JSONHandleMessage(String type, JSONMessageWrapper jsonMessageWrapper) {
            if (JSONHandlers.containsKey(type)) {
                JSONHandlers.get(type).accept(jsonMessageWrapper);
            } else {
                System.out.println("No handler for message type: " + type);
            }
        }

        private void nameType(JSONMessageWrapper jsonMessageWrapper) {
            User user = jsonMessageWrapper.getUser();
            JSONObject jsonObject = jsonMessageWrapper.getJsonObject();
            String oldName;
            String newName;
            newName = jsonObject.getString("name");
            oldName = user.getName();
            user.setName(newName);
            String text = oldName + "의 이름이 " + newName + "으로 변경되었습니다.";
            if (user.isParticipating()) {
                systemMessageType(user, null, text, null);
            } else {
                systemMessageType(user, text, null, null);
            }
        }

        private void createRoomType(JSONMessageWrapper jsonMessageWrapper) {
            JSONObject jsonObject = jsonMessageWrapper.getJsonObject();
            User user = jsonMessageWrapper.getUser();
            String text;
            if (user.isParticipating()) {
                text = "대화 방에 있을 때는 방을 개설 할 수 없습니다.";
            } else {
                String title = jsonObject.getString("title");
                Room room = roomManager.createRoom(user, title);
                text = "방제[" + title + "] 방에 입장했습니다.";
                System.out.println("room.getMembers() = " + room.getMembers());
            }
            systemMessageType(user, text, null, null);
        }

        private void roomsType(JSONMessageWrapper jsonMessageWrapper) {
            User user = jsonMessageWrapper.getUser();
            ConcurrentLinkedQueue<Room> rooms = roomManager.getRooms();
            if (rooms.isEmpty()) {  //개설된 방이 없으면 systemMessage
                String text = "개설된 방이 없습니다.";
                systemMessageType(user, text, null, null);
            } else {
                JSONObject roomsResultObject = new JSONObject();  //실제 전달할 JSONObject
                roomsResultObject.put("type", "SCRoomsResult");
                JSONArray roomsJsonArray = new JSONArray();       //Rooms 배열
                for (Room room : rooms) {
                    JSONObject roomJsonObject = new JSONObject(); //RoomInfo
                    JSONArray nameJSONArray = new JSONArray();    //RoomInfo의 names 필드
                    for (User members : room.getMembers()) {
                        nameJSONArray.put(members.getName());
                    }
                    roomJsonObject.put("roomId", room.getId());
                    roomJsonObject.put("title", room.getName());
                    roomJsonObject.put("members", nameJSONArray);
                    roomsJsonArray.put(roomJsonObject);
                }
                roomsResultObject.put("rooms", roomsJsonArray);
                ByteBuffer roomBuffer = JSONToByteBuffer(roomsResultObject);
                SocketChannel socketChannel = user.getSocketChannel();
                try {
                    socketChannel.write(roomBuffer);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        private void joinRoom(JSONMessageWrapper jsonMessageWrapper) {
            User user = jsonMessageWrapper.getUser();
            JSONObject jsonObject = jsonMessageWrapper.getJsonObject();
            int roomId = jsonObject.getInt("roomId");
            String text;
            if (user.isParticipating()) {
                text = "대화방에 있을 때는 다른 방에 들어갈 수 없습니다.";
                systemMessageType(user, text, null, null);
            } else if (!roomManager.joinRoom(user, roomId)) { //방이 존재하지 않을 때
                text = "대화방이 존재하지 않습니다.";
                systemMessageType(user, text, null, null);
            } else {
                Room room = roomManager.findRoom(roomId);
                text = "방제[" + room.getName() + "] 방에 입장했습니다.";
                String broadCastText = "[" + user.name + "] 님이 입장했습니다.";
                systemMessageType(user, text, broadCastText, null);
            }
        }
        private void leaveRoom(JSONMessageWrapper jsonMessageWrapper) {
            User user = jsonMessageWrapper.getUser();
            String text;
            if (!user.isParticipating()) {
                text = "현재 대화방에 들어가 있지 않습니다.";
                systemMessageType(user, text, null, null);
            } else {
                Room participatingRoom = user.getParticipatingRoom();
                text = "방제[" + participatingRoom.getName() + "] 대화방에서 퇴장했습니다.";
                String broadCastText = "[" + user.name + "] 님이 퇴장했습니다.";
                Room leftRoom = roomManager.leaveRoom(user);
                if (leftRoom.getMembers().size() == 0) {
                    roomManager.removeRoom(leftRoom);
                }
                systemMessageType(user, text, broadCastText, leftRoom);
            }
        }
        private void CSChatType(JSONMessageWrapper jsonMessageWrapper) {
            User user = jsonMessageWrapper.getUser();
            String text;
            if (!user.isParticipating()) {
                text = "현재 대화방에 들어가 있지 않습니다.";
                systemMessageType(user, text, null, null);
            } else {
                String chatText = jsonMessageWrapper.getJsonObject().getString("text");
                JSONObject chatJsonObject = new JSONObject();
                chatJsonObject.put("type", "SCChat");
                chatJsonObject.put("member", user.getName());
                chatJsonObject.put("text", chatText);
                ByteBuffer chatBuffer = JSONToByteBuffer(chatJsonObject);

                Room participatingRoom = user.getParticipatingRoom();
                ArrayList<User> members = participatingRoom.getMembers();
                for (User member : members) {
                    SocketChannel socketChannel = member.getSocketChannel();
                    while (chatBuffer.hasRemaining()) {   //버퍼에 남은게 없을때까지 보내기
                        try {
                            socketChannel.write(chatBuffer);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }
                    chatBuffer.flip();
                }
            }
        }
        private void shutdownType(JSONMessageWrapper jsonMessageWrapper) {
            shutdown();
        }
        public void systemMessageType(User user, String text, String broadCastText, Room leftRoom) {
            SocketChannel socketChannel = user.getSocketChannel();
            JSONObject jsonObjectOne = new JSONObject();
            JSONObject jsonObjectMembers = new JSONObject();
            if (text != null) {
                jsonObjectOne.put("text", text);
                jsonObjectOne.put("type", "SCSystemMessage");
                ByteBuffer buffer = JSONToByteBuffer(jsonObjectOne);
                try {
                    socketChannel.write(buffer);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
            if (broadCastText != null) {
                jsonObjectMembers.put("text", broadCastText);
                jsonObjectMembers.put("type", "SCSystemMessage");
                ByteBuffer buffer = JSONToByteBuffer(jsonObjectMembers);
                Room participatingRoom = user.getParticipatingRoom();
                if (participatingRoom == null) {
                    participatingRoom = leftRoom;
                }
                for (User member : participatingRoom.getMembers()) {
                    SocketChannel memberSocketChannel = member.getSocketChannel();
                    try {
                        memberSocketChannel.write(buffer);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }

        public static ByteBuffer JSONToByteBuffer(JSONObject jsonObject) {
            String jsonStr = jsonObject.toString();
            byte[] message = jsonStr.getBytes();
            ByteBuffer buffer = ByteBuffer.allocate(2 + message.length);
            short header = (short) message.length;
            buffer.putShort(header);
            buffer.put(message);
            buffer.flip();
            return buffer;
        }

        public void protobufHandleMessage(MessagePb.Type.MessageType type, ProtoMessageWrapper ProtoMessageWrapper) {
            if (protobufHandlers.containsKey(type)) {
                protobufHandlers.get(type).accept(ProtoMessageWrapper);
            } else {
                System.out.println("No handler for message type: " + type);
            }
        }

        private void nameType(ProtoMessageWrapper ProtoMessageWrapper) {
            String oldName;
            String newName;
            MessagePb.CSName namePb;
            try {
                namePb = MessagePb.CSName.parseFrom(ProtoMessageWrapper.getMessage());

            } catch (InvalidProtocolBufferException e) {
                throw new RuntimeException(e);
            }
            newName = namePb.getName().trim();
            User user = ProtoMessageWrapper.getUser();
            oldName = user.getName();
            user.setName(newName);
            String text = oldName + "의 이름이 " + newName + "으로 변경되었습니다.";
            if (user.isParticipating()) {
                systemMessageType(ProtoMessageWrapper, null, text, null);
            } else {
                systemMessageType(ProtoMessageWrapper, text, null, null);
            }

        }

        private void roomsType(ProtoMessageWrapper ProtoMessageWrapper) {
            Map<String, Object> fieldValues = getFieldsOfTypeWithEnum("SC_ROOMS_RESULT");
            DynamicMessage message = null;
            try {
                message = protobufCreator.createMessage(MessagePb.Type.getDescriptor(), fieldValues);
            } catch (Descriptors.DescriptorValidationException e) {
                throw new RuntimeException(e);
            }
            ByteBuffer typeBuffer = createDynamicMessageBuffer(message);
            fieldValues.clear();

            ConcurrentLinkedQueue<Room> rooms = roomManager.getRooms();
            if (rooms.isEmpty()) {  //개설된 방이 없으면 systemMessage
                String text = "개설된 방이 없습니다.";
                systemMessageType(ProtoMessageWrapper, text, null, null);
            } else {
                Descriptors.Descriptor descriptor = MessagePb.SCRoomsResult.getDescriptor();
                DynamicMessage.Builder dynamicBuilder = DynamicMessage.newBuilder(descriptor);
                for (Room room : rooms) {
                    MessagePb.SCRoomsResult.RoomInfo.Builder roomInfoBuilder = MessagePb.SCRoomsResult.RoomInfo.newBuilder();
                    for (User user : room.getMembers()) {
                        roomInfoBuilder.addMembers(user.getName());
                    }
                    MessagePb.SCRoomsResult.RoomInfo roomInfo = roomInfoBuilder.setRoomId(room.getId()).setTitle(room.getName()).build();
                    dynamicBuilder.addRepeatedField(descriptor.findFieldByName("rooms"), roomInfo);

                }
                DynamicMessage SCRoomResultMessage = dynamicBuilder.build();

                typeBuffer.flip();
                while (typeBuffer.hasRemaining()) {
                    try {
                        ByteBuffer SCRoomsResultBuffer = createDynamicMessageBuffer(SCRoomResultMessage);
                        SocketChannel socketChannel = ProtoMessageWrapper.getUser().getSocketChannel();
                        SCRoomsResultBuffer.flip();

                        socketChannel.write(typeBuffer);
                        socketChannel.write(SCRoomsResultBuffer);
                    } catch (IOException | NullPointerException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }

        private void createRoomType(ProtoMessageWrapper ProtoMessageWrapper) {
            try {
                MessagePb.CSCreateRoom CS_CreateRoom = MessagePb.CSCreateRoom.parseFrom(ProtoMessageWrapper.getMessage());
                User user = ProtoMessageWrapper.getUser();
                String text;
                if (user.isParticipating()) {
                    text = "대화 방에 있을 때는 방을 개설 할 수 없습니다.";
                } else {
                    String title = CS_CreateRoom.getTitle();
                    Room room = roomManager.createRoom(user, title);
                    text = "방제[" + title + "] 방에 입장했습니다.";
                    System.out.println("room.getMembers() = " + room.getMembers());
                }
                systemMessageType(ProtoMessageWrapper, text, null, null);


            } catch (InvalidProtocolBufferException e) {
                throw new RuntimeException(e);
            }
        }

        private void joinRoom(ProtoMessageWrapper ProtoMessageWrapper) {
            try {
                MessagePb.CSJoinRoom CS_JoinRoom = MessagePb.CSJoinRoom.parseFrom(ProtoMessageWrapper.getMessage());
                int roomId = CS_JoinRoom.getRoomId();
                User user = ProtoMessageWrapper.getUser();
                String text;
                if (user.isParticipating()) {
                    text = "대화방에 있을 때는 다른 방에 들어갈 수 없습니다.";
                    systemMessageType(ProtoMessageWrapper, text, null, null);
                } else if (!roomManager.joinRoom(user, roomId)) { //방이 존재하지 않을 때
                    text = "대화방이 존재하지 않습니다.";
                    systemMessageType(ProtoMessageWrapper, text, null, null);
                } else {
                    Room room = roomManager.findRoom(roomId);
                    text = "방제[" + room.getName() + "] 방에 입장했습니다.";
                    String broadCastText = "[" + user.name + "] 님이 입장했습니다.";
                    systemMessageType(ProtoMessageWrapper, text, broadCastText, null);
                }
            } catch (InvalidProtocolBufferException e) {
                throw new RuntimeException(e);
            }
        }

        private void leaveRoom(ProtoMessageWrapper ProtoMessageWrapper) {
            User user = ProtoMessageWrapper.getUser();
            String text;
            if (!user.isParticipating()) {
                text = "현재 대화방에 들어가 있지 않습니다.";
                systemMessageType(ProtoMessageWrapper, text, null, null);
            } else {
                Room participatingRoom = user.getParticipatingRoom();
                text = "방제[" + participatingRoom.getName() + "] 대화방에서 퇴장했습니다.";
                String broadCastText = "[" + user.name + "] 님이 퇴장했습니다.";
                Room leftRoom = roomManager.leaveRoom(user);
                if (leftRoom.getMembers().size() == 0) {
                    roomManager.removeRoom(leftRoom);
                }
                systemMessageType(ProtoMessageWrapper, text, broadCastText, leftRoom);
            }
        }

        private void CSChatType(ProtoMessageWrapper ProtoMessageWrapper) {
            User user = ProtoMessageWrapper.getUser();
            String text;
            if (!user.isParticipating()) {
                text = "현재 대화방에 들어가 있지 않습니다.";
                systemMessageType(ProtoMessageWrapper, text, null, null);
            } else {
                Map<String, Object> typeFieldValues = getFieldsOfTypeWithEnum("SC_CHAT");
                Map<String, Object> chatFieldValues = new HashMap<>();
                chatFieldValues.put("member", user.getName());

                DynamicMessage typeMessage = null;
                DynamicMessage SC_ChatMessage = null;
                byte[] message = ProtoMessageWrapper.getMessage();
                String textField = new String(message, StandardCharsets.UTF_8);
                chatFieldValues.put("text", textField);
                try {
                    typeMessage = protobufCreator.createMessage(MessagePb.Type.getDescriptor(), typeFieldValues);
                    SC_ChatMessage = protobufCreator.createMessage(MessagePb.SCChat.getDescriptor(), chatFieldValues);
                } catch (Descriptors.DescriptorValidationException e) {
                    throw new RuntimeException(e);
                }
                ByteBuffer typeBuffer = createDynamicMessageBuffer(typeMessage);
                ByteBuffer chatBuffer = createDynamicMessageBuffer(SC_ChatMessage);

                Room participatingRoom = user.getParticipatingRoom();
                ArrayList<User> members = participatingRoom.getMembers();
                for (User member : members) {
                    typeBuffer.rewind(); //set position 0
                    chatBuffer.rewind();
                    SocketChannel socketChannel = member.getSocketChannel();
                    while (typeBuffer.hasRemaining()) {
                        try {
                            socketChannel.write(typeBuffer);
                            socketChannel.write(chatBuffer);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
            }
        }

        private void shutdownType(ProtoMessageWrapper ProtoMessageWrapper) {
            shutdown();
        }
        private void shutdown(){
            try {
                //소켓 연결 종료
                for (Map.Entry<SocketChannel, User> entry : clients.entrySet()) { //모든 클라이언트 소켓연결 끊기
                    SocketChannel socketChannel = entry.getKey();
                    socketChannel.close();
                }
                serverSocketChannel.close();                                    //서버소켓 연결 끊기
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            try {
                threadPool.stop();                                              //스레드 정리
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            System.exit(0);
        }

        private void systemMessageType(ProtoMessageWrapper ProtoMessageWrapper, String text, String broadCastText, Room leftRoom) {

            Map<String, Object> fieldValues = getFieldsOfTypeWithEnum("SC_SYSTEM_MESSAGE");
            DynamicMessage typeMessage = null;
            try {
                typeMessage = protobufCreator.createMessage(MessagePb.Type.getDescriptor(), fieldValues);
            } catch (Descriptors.DescriptorValidationException e) {
                throw new RuntimeException(e);
            }
            ByteBuffer typeBuffer = createDynamicMessageBuffer(typeMessage);
            fieldValues.clear();
            // SystemMessage 중 개인 메세지와 방 메세지 둘의 유무에 따라 각각 보내기
            SocketChannel socketChannel = ProtoMessageWrapper.getUser().getSocketChannel();
            if (text != null) {
                fieldValues.put("text", text);
                DynamicMessage SCSystemMessageMessage = null;
                try {
                    SCSystemMessageMessage = protobufCreator.createMessage(MessagePb.SCSystemMessage.getDescriptor(), fieldValues);
                } catch (Descriptors.DescriptorValidationException e) {
                    throw new RuntimeException(e);
                }
                ByteBuffer SCSystemMessageBuffer = createDynamicMessageBuffer(SCSystemMessageMessage);
                fieldValues.clear();
                sendUserSystemMessage(typeBuffer, SCSystemMessageBuffer, socketChannel);
            }
            if (broadCastText != null) {
                fieldValues.put("text", broadCastText);
                DynamicMessage SCBroadCastSystemMessageMessage = null;
                try {
                    SCBroadCastSystemMessageMessage = protobufCreator.createMessage(MessagePb.SCSystemMessage.getDescriptor(), fieldValues);
                } catch (Descriptors.DescriptorValidationException e) {
                    throw new RuntimeException(e);
                }
                ByteBuffer SCSystemMessageBuffer = createDynamicMessageBuffer(SCBroadCastSystemMessageMessage);
                fieldValues.clear();
                Room participatingRoom = ProtoMessageWrapper.getUser().getParticipatingRoom();
                if (participatingRoom == null) {
                    participatingRoom = leftRoom;
                }
                sendMembersSystemMessage(typeBuffer, SCSystemMessageBuffer, socketChannel, participatingRoom);
            }
        }

        public static Map<String, Object> getFieldsOfTypeWithEnum(String enumType) {
            HashMap<String, Object> fieldValues = new HashMap<>();
            Descriptors.EnumDescriptor descriptor = MessagePb.Type.MessageType.getDescriptor();
            Descriptors.EnumValueDescriptor myEnumValue = descriptor.findValueByName(enumType);
            fieldValues.put("type", myEnumValue);
            return fieldValues;
        }

        public static ByteBuffer createDynamicMessageBuffer(DynamicMessage message) {
            byte[] serializedMessage = serialize(message);
            short messageSize = (short) serializedMessage.length;
            ByteBuffer buffer = putBuffer(messageSize, serializedMessage);
            return buffer;
        }

        public static byte[] serialize(DynamicMessage dynamicMessage) {
            ByteString byteString = dynamicMessage.toByteString(); //직렬화
            byte[] messageBytes = byteString.toByteArray();
            return messageBytes;
        }

        public static ByteBuffer putBuffer(short messageSize, byte[] serializedMessage) {
            ByteBuffer buffer = ByteBuffer.allocate(2 + messageSize); //버퍼에 사이즈 및 메시지 저장
            buffer.putShort(messageSize);
            buffer.put(serializedMessage);
            return buffer;
        }

        public static void sendUserSystemMessage(ByteBuffer typeBuffer, ByteBuffer SCSystemMessageBuffer, SocketChannel socketChannel) {
            typeBuffer.flip();
            SCSystemMessageBuffer.flip();
            while (typeBuffer.hasRemaining()) {
                try {
                    socketChannel.write(typeBuffer);
                    socketChannel.write(SCSystemMessageBuffer);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        public static void sendMembersSystemMessage(ByteBuffer typeBuffer, ByteBuffer SCSystemMessageBuffer, SocketChannel socketChannel, Room room) {
            ArrayList<User> members = room.getMembers();
            for (User user : members) {
                typeBuffer.rewind(); //set position 0
                SCSystemMessageBuffer.rewind();
                socketChannel = user.getSocketChannel();
                while (typeBuffer.hasRemaining()) {
                    try {
                        socketChannel.write(typeBuffer);
                        socketChannel.write(SCSystemMessageBuffer);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
    }
}
