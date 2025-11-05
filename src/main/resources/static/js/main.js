'use strict';

var roomSelectionPage = document.querySelector('#room-selection-page');
var chatPage = document.querySelector('#chat-page');
var messageForm = document.querySelector('#messageForm');
var messageInput = document.querySelector('#message');
var messageArea = document.querySelector('#messageArea');
var connectingElement = document.querySelector('.connecting');
var roomListElement = document.querySelector('#room-list');
var chatRoomNameElement = document.querySelector('#chat-room-name');
var startChattingButton = document.querySelector('#start-chatting');

var stompClient = null;
var selectedRoomId = null;

var colors = [
    '#2196F3', '#32c787', '#00BCD4', '#ff5652',
    '#ffc107', '#ff85af', '#FF9800', '#39bbb0'
];

var createRoomForm = document.querySelector('#createRoomForm');

function loadRooms() {
    roomListElement.innerHTML = ''; // Clear the list before loading
    fetch('/api/chatrooms')
        .then(response => response.json())
        .then(rooms => {
            rooms.forEach(room => {
                var roomElement = document.createElement('li');
                roomElement.textContent = room.name;
                roomElement.dataset.roomId = room.roomId;
                roomElement.dataset.roomName = room.name;

                roomElement.addEventListener('click', function() {
                    selectedRoomId = this.dataset.roomId;
                    var roomName = this.dataset.roomName;
                    chatRoomNameElement.textContent = roomName;
                    // Highlight selected room
                    document.querySelectorAll('#room-list li').forEach(el => el.classList.remove('active'));
                    this.classList.add('active');
                });

                roomListElement.appendChild(roomElement);
            });
        });
}

function createRoom(event) {
    var roomName = document.querySelector('#room-name').value.trim();

    if(roomName) {
        var roomId = new Date().getTime(); // Simple unique ID
        fetch('/api/chatrooms', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify({roomId: roomId, name: roomName}),
        })
        .then(response => {
            if(response.status === 201) {
                document.querySelector('#room-name').value = '';
                loadRooms();
            }
        });
    }
    event.preventDefault();
}

function connect(event) {
    if (!selectedRoomId) {
        alert('Please select a room first.');
        event.preventDefault();
        return;
    }

    roomSelectionPage.classList.add('hidden');
    chatPage.classList.remove('hidden');

    var socket = new SockJS('/ws');
    stompClient = Stomp.over(socket);

    stompClient.connect({}, onConnected, onError);
    event.preventDefault();
}


function onConnected() {
    // Subscribe to the selected room Topic
    stompClient.subscribe('/topic/rooms/' + selectedRoomId, onMessageReceived);

    // Fetch chat history
    fetch('/api/chat/' + selectedRoomId + '/messages')
        .then(response => response.json())
        .then(messages => {
            messages.forEach(message => {
                displayMessage(message.type, message.sender, message.content);
            });

            // Tell your username to the server
            stompClient.send('/app/chat/' + selectedRoomId + '/addUser',
                {},
                JSON.stringify({type: 'JOIN'})
            )

            connectingElement.classList.add('hidden');
        });
}


function onError(error) {
    connectingElement.textContent = 'Could not connect to WebSocket server. Please refresh this page to try again!';
    connectingElement.style.color = 'red';
}


function sendMessage(event) {
    var messageContent = messageInput.value.trim();

    if(messageContent && stompClient) {
        var chatMessage = {
            content: messageInput.value,
            type: 'CHAT'
        };

        stompClient.send('/app/chat/' + selectedRoomId + '/sendMessage', {}, JSON.stringify(chatMessage));
        messageInput.value = '';
    }
    event.preventDefault();
}

function displayMessage(type, sender, content) {
    var messageElement = document.createElement('li');

    if(type === 'JOIN') {
        messageElement.classList.add('event-message');
        content = sender + ' joined!';
    } else if (type === 'LEAVE') {
        messageElement.classList.add('event-message');
        content = sender + ' left!';
    } else {
        messageElement.classList.add('chat-message');

        var usernameElement = document.createElement('span');
        usernameElement.classList.add('chat-username-colored');
        var usernameText = document.createTextNode(sender);
        usernameElement.appendChild(usernameText);
        usernameElement.style['background-color'] = getAvatarColor(sender);

        messageElement.appendChild(usernameElement);
    }

    var textElement = document.createElement('p');
    var messageText = document.createTextNode(content);
    textElement.appendChild(messageText);

    messageElement.appendChild(textElement);

    messageArea.appendChild(messageElement);
    messageArea.scrollTop = messageArea.scrollHeight;
}

function onMessageReceived(payload) {
    var message = JSON.parse(payload.body);
    displayMessage(message.type, message.sender, message.content);
}


function getAvatarColor(messageSender) {
    var hash = 0;
    for (var i = 0; i < messageSender.length; i++) {
        hash = 31 * hash + messageSender.charCodeAt(i);
    }

    var index = Math.abs(hash % colors.length);
    return colors[index];
}

var leaveButton = document.querySelector('#leaveButton');

function leaveChat(event) {
    if (stompClient) {
        stompClient.disconnect();
    }
    chatPage.classList.add('hidden');
    roomSelectionPage.classList.remove('hidden');
    messageArea.innerHTML = '';
    selectedRoomId = null;
    document.querySelectorAll('#room-list li').forEach(el => el.classList.remove('active'));
    event.preventDefault();
}

document.addEventListener('DOMContentLoaded', loadRooms);
startChattingButton.addEventListener('click', connect, true)
messageForm.addEventListener('submit', sendMessage, true);
createRoomForm.addEventListener('submit', createRoom, true);
leaveButton.addEventListener('click', leaveChat, true);