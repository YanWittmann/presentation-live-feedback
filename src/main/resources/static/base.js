const apiBaseUrl = '';

function doPost(url, param, callback = data => {
}, failure = err => {
}) {
    if (callback === undefined) {
        callback = data => {
        };
    }
    if (failure === undefined) {
        failure = err => {
        };
    }
    fetch(apiBaseUrl + url, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify(param)
    })
        .then(response => {
            if (!response.ok) {
                throw new Error('Network response was not ok.');
            }
            return response.json();
        })
        .then(data => {
            console.log('✔️', url, param, data);
            callback(data);
        })
        .catch(error => {
            console.log('❌', url, param, error.message);
            failure(error.message);
        });
}

function doGet(url, callback = data => {
}, failure = err => {
}) {
    if (callback === undefined) {
        callback = data => {
        };
    }
    if (failure === undefined) {
        failure = err => {
        };
    }
    fetch(apiBaseUrl + url)
        .then(response => {
            if (!response.ok) {
                throw new Error('Network response was not ok.');
            }
            return response.json();
        })
        .then(data => {
            callback(data);
        })
        .catch(error => {
            failure(error.message);
        });
}

function generateSystemFingerprint() {
    let hash = 0;

    {
        const canvas = document.createElement('canvas');
        const ctx = canvas.getContext('2d');
        const txt = '';
        const font = "Arial";
        ctx.textBaseline = "top";
        ctx.font = "14px " + font;
        ctx.textBaseline = "alphabetic";
        ctx.fillStyle = "#f60";
        ctx.fillRect(125, 1, 62, 20);
        ctx.fillStyle = "#069";
        ctx.fillText(txt, 2, 15);
        ctx.fillStyle = "rgba(102, 204, 0, 0.7)";
        ctx.fillText(txt, 4, 17);

        const b64 = canvas.toDataURL().replace("data:image/png;base64,", "");
        const bin = atob(b64);
        for (let i = 0; i < bin.length; i++) {
            hash += bin.charCodeAt(i);
        }
    }

    {
        const userAgent = window.navigator.userAgent;
        for (let i = 0; i < userAgent.length; i++) {
            hash *= userAgent.charCodeAt(i);
        }
    }

    const hashString = hash.toString(16).replaceAll(/0+$/g, '');
    const paddedHashString = hashString.padEnd(32, '0');
    let result = '';
    for (let i = 0; i < 32; i++) {
        result += paddedHashString.charAt(i % hashString.length);
        if (i === 7 || i === 11 || i === 15 || i === 19) {
            result += '-';
        }
    }
    return result;
}

const systemFingerprint = generateSystemFingerprint();


const tooltipsMap = new WeakMap();

document.addEventListener('DOMContentLoaded', () => {
    updateBsTooltips();
});

function updateBsTooltips(elements = null) {
    const tooltipTriggerList = elements || [].slice.call(document.querySelectorAll('[data-bs-toggle="tooltip"]'));

    tooltipTriggerList.forEach((tooltipTriggerEl) => {
        if (tooltipsMap.has(tooltipTriggerEl)) {
            const existingTooltip = tooltipsMap.get(tooltipTriggerEl);
            existingTooltip.dispose();
        }

        const tooltipInstance = new bootstrap.Tooltip(tooltipTriggerEl);
        tooltipsMap.set(tooltipTriggerEl, tooltipInstance);
    });
}

function determineWebSocketUrl() {
    const protocol = (window.location.protocol === 'https:') ? 'wss://' : 'ws://';
    const host = window.location.hostname;
    const port = window.location.port ? ':' + window.location.port : '';
    return protocol + host + port + '/ws';
}

let socket = null;

function getWebSocket() {
    if (socket === null) {
        socket = new WebSocket(determineWebSocketUrl());
    }
    return socket;
}

function parseUrlParameters() {
    const urlParams = new URLSearchParams(window.location.search);
    const params = {};
    for (const [key, value] of urlParams) {
        params[key] = value;
    }
    return params;
}

function clearUrlParameters(parameters = []) {
    const urlParams = new URLSearchParams(window.location.search);
    for (const parameter of parameters) {
        urlParams.delete(parameter);
    }
    window.history.replaceState({}, document.title, window.location.pathname + '?' + urlParams.toString());
}

let genericModalInstance = null;

function showGenericModal(title, body, closeCallback = () => {
    console.log('generic-modal close', title, body);
}) {
    const modalElement = document.getElementById('generic-modal');
    if (!modalElement) {
        alert(title + '\n\n' + body);
        closeCallback();
        return;
    }

    if (!genericModalInstance) {
        genericModalInstance = new bootstrap.Modal(modalElement);
    }

    document.getElementById('generic-modal-title').innerText = title;
    document.getElementById('generic-modal-body').innerText = body;

    modalElement.addEventListener('hidden.bs.modal', function onModalHidden() {
        closeCallback();
        modalElement.removeEventListener('hidden.bs.modal', onModalHidden);
    });

    genericModalInstance.show();
}

/**
 * Human readable elapsed or remaining time (example: 3 minutes ago)
 * @param  {Date|Number|String} date A Date object, timestamp or string parsable with Date.parse()
 * @return {string} Human readable elapsed or remaining time
 * @author github.com/victornpb
 * @see https://stackoverflow.com/a/67338038/938822
 */
function fromNow(date) {
    const SECOND = 1000;
    const MINUTE = 60 * SECOND;
    const HOUR = 60 * MINUTE;
    const DAY = 24 * HOUR;
    const WEEK = 7 * DAY;
    const MONTH = 30 * DAY;
    const YEAR = 365 * DAY;
    const units = [
        {
            max: 5 * SECOND,
            divisor: 1,
            past1: 'just now',
            pastN: 'just now',
            future1: 'just now',
            futureN: 'just now'
        },
        {
            max: MINUTE,
            divisor: SECOND,
            past1: 'a second ago',
            pastN: '# seconds ago',
            future1: 'in a second',
            futureN: 'in # seconds'
        },
        {
            max: HOUR,
            divisor: MINUTE,
            past1: 'a minute ago',
            pastN: '# minutes ago',
            future1: 'in a minute',
            futureN: 'in # minutes'
        },
        {
            max: DAY,
            divisor: HOUR,
            past1: 'an hour ago',
            pastN: '# hours ago',
            future1: 'in an hour',
            futureN: 'in # hours'
        },
        {
            max: WEEK,
            divisor: DAY,
            past1: 'yesterday',
            pastN: '# days ago',
            future1: 'tomorrow',
            futureN: 'in # days'
        },
        {
            max: 4 * WEEK,
            divisor: WEEK,
            past1: 'last week',
            pastN: '# weeks ago',
            future1: 'in a week',
            futureN: 'in # weeks'
        },
        {
            max: YEAR,
            divisor: MONTH,
            past1: 'last month',
            pastN: '# months ago',
            future1: 'in a month',
            futureN: 'in # months'
        },
        {
            max: 100 * YEAR,
            divisor: YEAR,
            past1: 'last year',
            pastN: '# years ago',
            future1: 'in a year',
            futureN: 'in # years'
        },
        {
            max: 1000 * YEAR,
            divisor: 100 * YEAR,
            past1: 'last century',
            pastN: '# centuries ago',
            future1: 'in a century',
            futureN: 'in # centuries'
        },
        {
            max: Infinity,
            divisor: 1000 * YEAR,
            past1: 'last millennium',
            pastN: '# millennia ago',
            future1: 'in a millennium',
            futureN: 'in # millennia'
        },
    ];
    const diff = Date.now() - (typeof date === 'object' ? date : new Date(date)).getTime();
    const diffAbs = Math.abs(diff);
    for (const unit of units) {
        if (diffAbs < unit.max) {
            const isFuture = diff < 0;
            const x = Math.round(Math.abs(diff) / unit.divisor);
            if (x <= 1) return isFuture ? unit.future1 : unit.past1;
            return (isFuture ? unit.futureN : unit.pastN).replace('#', x);
        }
    }
}
