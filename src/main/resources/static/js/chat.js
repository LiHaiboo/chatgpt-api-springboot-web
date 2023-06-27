const messagesContainer = document.getElementById('messages');
const input = document.getElementById('input');
const sendButton = document.getElementById('send');
var qaIdx = 0, answers = {}, answerContent = '', answerWords = [];
var codeStart = false, lastWord = '', lastLastWord = '';
var typingTimer = null, typing = false, typingIdx = 0, contentIdx = 0, contentEnd = false;
var isMinimax = false;

//markdown解析，代码高亮设置
marked.setOptions({
    highlight: function (code, language) {
        const validLanguage = hljs.getLanguage(language) ? language : 'javascript';
        return hljs.highlight(code, {language: validLanguage}).value;
    },
});

function initializeChat() {
    const inputValue = '哈喽！（此问题仅用于demo演示）';

    const question = document.createElement('div');
    question.setAttribute('class', 'message question');
    question.setAttribute('id', 'question-' + qaIdx);
    question.innerHTML = marked.parse(inputValue);
    messagesContainer.appendChild(question);

    const answer = document.createElement('div');
    answer.setAttribute('class', 'message answer');
    answer.setAttribute('id', 'answer-' + qaIdx);
    answer.innerHTML = marked.parse('AI思考中……');
    messagesContainer.appendChild(answer);

    answers[qaIdx] = document.getElementById('answer-' + qaIdx);

    input.value = '';
    input.disabled = true;
    sendButton.disabled = true;
    adjustInputHeight();

    //typingTimer = setInterval(typingWords, 50);

    getAnswer(inputValue);

}

/*
* 开关控制
*/
document.getElementById("toggle").addEventListener("change",function() {
    isMinimax = this.checked;

    //清空聊天记录
    for(let i = 0; i < qaIdx; i++) {
        if(document.getElementById('question-'+i) != null) {
            document.getElementById('question-'+i).remove()
        }
        if(document.getElementById('answer-'+i) != null) {
            document.getElementById('answer-'+i).remove()
        }

    }

    console.log(isMinimax);
    initializeChat();
});

//在输入时和获取焦点后自动调整输入框高度
input.addEventListener('input', adjustInputHeight);
input.addEventListener('focus', adjustInputHeight);

// 自动调整输入框高度
function adjustInputHeight() {
    input.style.height = 'auto'; // 将高度重置为 auto
    input.style.height = (input.scrollHeight + 2) + 'px';
}

function sendMessage() {
    const inputValue = input.value;
    if (!inputValue) {
        return;
    }

    const question = document.createElement('div');
    question.setAttribute('class', 'message question');
    question.setAttribute('id', 'question-' + qaIdx);
    question.innerHTML = marked.parse(inputValue);
    messagesContainer.appendChild(question);

    const answer = document.createElement('div');
    answer.setAttribute('class', 'message answer');
    answer.setAttribute('id', 'answer-' + qaIdx);
    answer.innerHTML = marked.parse('AI思考中……');
    messagesContainer.appendChild(answer);

    answers[qaIdx] = document.getElementById('answer-' + qaIdx);

    input.value = '';
    input.disabled = true;
    sendButton.disabled = true;
    adjustInputHeight();

    //typingTimer = setInterval(typingWords, 50);

    getAnswer(inputValue);
}

/*
* 1. 向后端传值(目前是通过eventSource todo：简单带参数访问url)，后端调用api拿到答案
* 2.  接收后端返回的answer/todo 接收后端主动返回的答案（较简单，JSP？java -> js）
* 3. 添加到AnswerWords数组，结束
* */
function getAnswer(inputValue) {
    //inputValue = encodeURIComponent(inputValue.replace(/\+/g, '{[$add$]}'));
    var url;
    if(isMinimax) {
        url = "/minimax?inputValue=" + inputValue;
    } else {
        url = "/sse?inputValue=" + inputValue;
    }
    const eventSource = new EventSource(url);

    eventSource.addEventListener("open", (event) => {
        console.log("连接已建立", JSON.stringify(event));
    });

    eventSource.addEventListener("message", (event) => {
        console.log("接收数据：", event);
        try {
            // var result = JSON.parse(event.data);
            // if(result.time && result.content ){
            //     answerWords.push(result.content);
            //     contentIdx += 1;
            // }
            var result = event.data;
            if (result != null) {
                answerWords.push(result);
                contentIdx += 1;

                answers[qaIdx].innerHTML = marked.parse(result);
                qaIdx += 1;
                input.disabled = false;
                sendButton.disabled = false;
                eventSource.close();
            }
        } catch (error) {
            console.log(error);
        }
    });

    eventSource.addEventListener("error", (event) => {
        console.error("发生错误：", JSON.stringify(event));
        console.log("连接已关闭", JSON.stringify(event.data));
        eventSource.close();
        contentEnd = true;
        console.log((new Date().getTime()), 'answer end');
    });

    eventSource.addEventListener("close", (event) => {
        console.log("连接已关闭", JSON.stringify(event.data));
        eventSource.close();
        contentEnd = true;
        console.log((new Date().getTime()), 'answer end');
    });
}


function typingWords() {
    if (contentEnd && contentIdx == typingIdx) {
        clearInterval(typingTimer);
        answerContent = '';
        answerWords = [];
        answers = [];
        qaIdx += 1;
        typingIdx = 0;
        contentIdx = 0;
        contentEnd = false;
        lastWord = '';
        lastLastWord = '';
        input.disabled = false;
        sendButton.disabled = false;
        console.log((new Date().getTime()), 'typing end');
        return;
    }
    if (contentIdx <= typingIdx) {
        return;
    }
    if (typing) {
        return;
    }
    typing = true;

    if (!answers[qaIdx]) {
        answers[qaIdx] = document.getElementById('answer-' + qaIdx);
    }

    const content = answerWords[typingIdx];
    if (content.indexOf('`') != -1) {
        if (content.indexOf('```') != -1) {
            codeStart = !codeStart;
        } else if (content.indexOf('``') != -1 && (lastWord + content).indexOf('```') != -1) {
            codeStart = !codeStart;
        } else if (content.indexOf('`') != -1 && (lastLastWord + lastWord + content).indexOf('```') != -1) {
            codeStart = !codeStart;
        }
    }

    lastLastWord = lastWord;
    lastWord = content;

    answerContent += content;
    answers[qaIdx].innerHTML = marked.parse(answerContent + (codeStart ? '\n\n```' : ''));

    typingIdx += 1;
    typing = false;
}

function handleClick(uid) {
    var url;
    if(isMinimax) {
        url = '/minimaxSingle'
    } else {
        url = '/gptSingle'
    }

    fetch(url, {
        method: 'POST', // or 'GET', 'PUT', 'DELETE', etc.
        headers: {
            'Content-Type': 'application/json' // set the appropriate content type
        },
        body: JSON.stringify({ uuid:  uid}) // include the data to send in the request body
    })
        //.then(response => response.json())
        .then(data => {
            // handle the response from the backend
            console.log(data);
        })
        .catch(error => {
            // handle any errors that occurred during the request
            console.error('Error:', error);
        });


    //创建聊天框
    const question = document.createElement('div');
    question.setAttribute('class', 'message question');
    question.setAttribute('id', 'question-' + qaIdx);
    question.innerHTML = marked.parse('发送商品数据（此消息仅用于demo演示）');
    messagesContainer.appendChild(question);

    const answer = document.createElement('div');
    answer.setAttribute('class', 'message answer');
    answer.setAttribute('id', 'answer-' + qaIdx);
    answer.innerHTML = marked.parse('模型已收到商品数据');
    messagesContainer.appendChild(answer);

    answers[qaIdx] = document.getElementById('answer-' + qaIdx);

    contentIdx += 1;
    qaIdx += 1;



}
