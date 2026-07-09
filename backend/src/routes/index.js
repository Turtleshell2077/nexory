const express    = require('express');
const router     = express.Router();
const { authenticate, requireAdmin } = require('../middleware/auth');
 
// Контроллеры
const authCtrl    = require('../controllers/authController');
const eventsCtrl  = require('../controllers/eventsController');
const chatsCtrl   = require('../controllers/chatsController');
const { getFriends, getFriendRequests, sendFriendRequest,
        acceptFriendRequest, removeFriend, cancelFriendRequest } = require('../controllers/otherControllers');
const { users: usersCtrl }   = require('../controllers/otherControllers');
const { support: supportCtrl } = require('../controllers/otherControllers');
const { reports: reportsCtrl } = require('../controllers/otherControllers');
const { uploadImage } = require('../controllers/uploadController');
const validate = require('../middleware/validate');
const v = require('../middleware/validators');

// ================================================================
// AUTH — публичные маршруты (без токена)
// ================================================================
router.post('/auth/register', v.auth.register, validate, authCtrl.register);
router.post('/auth/login',    v.auth.login, validate, authCtrl.login);
router.post('/auth/refresh',  authCtrl.refresh);  // нужен только refresh token
router.post('/auth/logout',   authCtrl.logout);   // можно и без токена
router.post('/auth/request-password-reset', v.auth.requestReset, validate, authCtrl.requestPasswordReset);
router.post('/auth/reset-password',          v.auth.resetPassword, validate, authCtrl.resetPassword);
router.post('/auth/verify-email',            authenticate, authCtrl.verifyEmail);
router.post('/auth/resend-verification',     authenticate, authCtrl.resendVerification);
 
// ================================================================
// EVENTS — защищённые
// ================================================================
router.get('/events',        authenticate, eventsCtrl.getEvents);
router.get('/events/my',     authenticate, eventsCtrl.getMyEvents);
router.get('/events/:id',    authenticate, eventsCtrl.getEventById);
router.post('/events',       authenticate, v.events.create, validate, eventsCtrl.createEvent);
router.put('/events/:id',    authenticate, v.events.update, validate, eventsCtrl.updateEvent);
router.post('/events/:id/join',  authenticate, eventsCtrl.joinEvent);
router.delete('/events/:id/leave', authenticate, eventsCtrl.leaveEvent);
router.put('/events/:id/participants/:userId',    authenticate, eventsCtrl.setParticipantRole);
router.delete('/events/:id/participants/:userId', authenticate, eventsCtrl.kickParticipant);
router.delete('/events/:id', authenticate, eventsCtrl.deleteEvent);
 
// ================================================================
// CHATS
// ================================================================
router.get('/chats',              authenticate, chatsCtrl.getMyChats);
router.post('/chats/direct',      authenticate, chatsCtrl.getOrCreateDirectChat);
router.get('/chats/:id/messages', authenticate, chatsCtrl.getMessages);
router.post('/chats/:id/messages', authenticate, v.chats.send, validate, chatsCtrl.sendMessage);
router.put('/chats/:id/avatar',   authenticate, chatsCtrl.updateChatAvatar);
router.patch('/chats/:id/flags',  authenticate, chatsCtrl.updateChatFlags);
router.delete('/chats/:id',       authenticate, chatsCtrl.deleteChat);
router.get('/chats/:id',          authenticate, chatsCtrl.getChatInfo);
 
// ================================================================
// FRIENDS
// ================================================================
router.get('/friends',             authenticate, getFriends);
router.get('/friends/requests',    authenticate, getFriendRequests);
router.post('/friends/request',    authenticate, sendFriendRequest);
router.post('/friends/accept',     authenticate, acceptFriendRequest);
router.delete('/friends/request/:id', authenticate, cancelFriendRequest);
router.delete('/friends/:id',      authenticate, removeFriend);
 
// ================================================================
// USERS
// ================================================================
router.get('/users/search',    authenticate, v.users.search, validate, usersCtrl.searchUsers);
router.get('/users/me',        authenticate, usersCtrl.getProfile);
router.put('/users/me',        authenticate, v.users.update, validate, usersCtrl.updateProfile);
router.put('/users/me/password',   authenticate, v.users.changePassword, validate, usersCtrl.changePassword);
router.put('/users/me/fcm-token',  authenticate, usersCtrl.updateFcmToken);
router.get('/users/me/allowed',    authenticate, usersCtrl.getAllowed);
router.put('/users/me/allowed',    authenticate, usersCtrl.setAllowed);
router.get('/users/:id',       authenticate, usersCtrl.getProfile); // чужой профиль
 
// ================================================================
// SUPPORT
// ================================================================
router.post('/support', authenticate, v.support.create, validate, supportCtrl.createTicket);

// ================================================================
// REPORTS — жалобы на контент (модерация)
// ================================================================
router.post('/reports', authenticate, v.reports.create, validate, reportsCtrl.createReport);
router.get('/reports',  authenticate, requireAdmin, reportsCtrl.listReports);

// ================================================================
// UPLOAD — загрузка изображений (аватары, обложки)
// ================================================================
router.post('/upload', authenticate, uploadImage);

module.exports = router;