const http = require('http');
const fs = require('fs');
const path = require('path');

const PORT = 3000;
const PUBLIC_DIR = path.join(__dirname, 'public');
const DATA_DIR = path.join(__dirname, 'data');

if (!fs.existsSync(DATA_DIR)) {
  fs.mkdirSync(DATA_DIR, { recursive: true });
}

const USERS_FILE = path.join(DATA_DIR, 'users.json');
const COLLEGES_FILE = path.join(DATA_DIR, 'colleges.json');
const KMZ_FILE = path.join(DATA_DIR, 'kmz_boundary.json');

// Helper to safely read JSON
function readJSON(file, defaultVal = []) {
  try {
    if (fs.existsSync(file)) {
      return JSON.parse(fs.readFileSync(file, 'utf8'));
    }
  } catch (err) {
    console.error('Error reading JSON from ' + file, err);
  }
  return defaultVal;
}

// Helper to safely write JSON
function writeJSON(file, data) {
  try {
    fs.writeFileSync(file, JSON.stringify(data, null, 2), 'utf8');
    return true;
  } catch (err) {
    console.error('Error writing JSON to ' + file, err);
    return false;
  }
}

const MIME_TYPES = {
  '.html': 'text/html; charset=utf-8',
  '.css': 'text/css; charset=utf-8',
  '.js': 'application/javascript; charset=utf-8',
  '.json': 'application/json; charset=utf-8',
  '.png': 'image/png',
  '.jpg': 'image/jpeg',
  '.jpeg': 'image/jpeg',
  '.svg': 'image/svg+xml',
  '.ico': 'image/x-icon',
  '.kmz': 'application/vnd.google-earth.kmz',
  '.kml': 'application/vnd.google-earth.kml+xml',
  '.xlsx': 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
  '.xls': 'application/vnd.ms-excel',
  '.csv': 'text/csv; charset=utf-8'
};

function readBody(req) {
  return new Promise((resolve, reject) => {
    let body = '';
    req.on('data', chunk => {
      body += chunk;
      if (body.length > 50 * 1024 * 1024) { // 50MB limit
        reject(new Error('Body too large'));
      }
    });
    req.on('end', () => {
      try {
        resolve(body ? JSON.parse(body) : {});
      } catch (e) {
        resolve({ raw: body });
      }
    });
    req.on('error', reject);
  });
}

function sendJSON(res, status, data) {
  res.writeHead(status, {
    'Content-Type': 'application/json; charset=utf-8',
    'Access-Control-Allow-Origin': '*',
    'Access-Control-Allow-Methods': 'GET, POST, PUT, DELETE, OPTIONS',
    'Access-Control-Allow-Headers': 'Content-Type, Authorization, x-user-role',
    'Cache-Control': 'no-cache, no-store, must-revalidate'
  });
  res.end(JSON.stringify(data));
}

const server = http.createServer(async (req, res) => {
  // CORS Preflight
  if (req.method === 'OPTIONS') {
    res.writeHead(204, {
      'Access-Control-Allow-Origin': '*',
      'Access-Control-Allow-Methods': 'GET, POST, PUT, DELETE, OPTIONS',
      'Access-Control-Allow-Headers': 'Content-Type, Authorization, x-user-role',
      'Access-Control-Max-Age': '86400'
    });
    res.end();
    return;
  }

  let parsedUrl = new URL(req.url, `http://${req.headers.host || 'localhost'}`);
  let pathname = decodeURIComponent(parsedUrl.pathname);

  // ================= API ROUTES =================
  if (pathname.startsWith('/api/')) {
    try {
      // 1. LOGIN
      if (pathname === '/api/login' && req.method === 'POST') {
        const { username, password } = await readBody(req);
        const users = readJSON(USERS_FILE, []);
        const cleanUser = String(username || '').trim().toLowerCase();
        const cleanPass = String(password || '').trim();

        const found = users.find(u => u.username.toLowerCase() === cleanUser && u.password === cleanPass);
        if (found) {
          const isAdmin = (found.role === 'admin') || (found.username.toLowerCase() === 'satish');
          sendJSON(res, 200, {
            success: true,
            user: {
              username: found.username,
              role: found.role || 'editor',
              name: found.name || found.username
            },
            adminToken: isAdmin ? 'unimap_admin_sec_99a8b' : null
          });
        } else {
          sendJSON(res, 401, { success: false, message: 'Invalid username or password. Please check your credentials.' });
        }
        return;
      }

      // Helper to verify admin token
      const checkAdminAuth = () => {
        const token = req.headers['x-admin-token'] || parsedUrl.searchParams.get('token');
        return token === 'unimap_admin_sec_99a8b';
      };

      // 2. GET /api/users (Admin Only)
      if (pathname === '/api/users' && req.method === 'GET') {
        if (!checkAdminAuth()) {
          sendJSON(res, 403, { success: false, message: 'Access Denied: Admin privileges required to view user credentials.' });
          return;
        }
        const users = readJSON(USERS_FILE, []);
        sendJSON(res, 200, users);
        return;
      }

      // 3. POST /api/users (Add or Update credentials - Admin Only)
      if (pathname === '/api/users' && req.method === 'POST') {
        if (!checkAdminAuth()) {
          sendJSON(res, 403, { success: false, message: 'Access Denied: Admin privileges required to manage credentials.' });
          return;
        }
        const { username, password, role, name } = await readBody(req);
        if (!username || !password) {
          sendJSON(res, 400, { success: false, message: 'Username and password are required' });
          return;
        }
        let users = readJSON(USERS_FILE, []);
        const targetUsername = String(username).trim();
        const idx = users.findIndex(u => u.username.toLowerCase() === targetUsername.toLowerCase());

        if (idx !== -1) {
          users[idx].password = String(password).trim();
          users[idx].role = role || users[idx].role || 'editor';
          users[idx].name = name || users[idx].name || targetUsername;
        } else {
          users.push({
            username: targetUsername,
            password: String(password).trim(),
            role: role || 'editor',
            name: name || targetUsername
          });
        }

        writeJSON(USERS_FILE, users);
        sendJSON(res, 200, { success: true, users });
        return;
      }

      // 4. DELETE /api/users/:username (Admin Only)
      if (pathname.startsWith('/api/users/') && req.method === 'DELETE') {
        if (!checkAdminAuth()) {
          sendJSON(res, 403, { success: false, message: 'Access Denied: Admin privileges required to delete credentials.' });
          return;
        }
        const userToDelete = decodeURIComponent(pathname.replace('/api/users/', '')).trim().toLowerCase();
        let users = readJSON(USERS_FILE, []);
        if (users.length <= 1) {
          sendJSON(res, 400, { success: false, message: 'Cannot delete the only remaining user account' });
          return;
        }
        users = users.filter(u => u.username.toLowerCase() !== userToDelete);
        writeJSON(USERS_FILE, users);
        sendJSON(res, 200, { success: true, users });
        return;
      }

      // 5. GET /api/colleges
      if (pathname === '/api/colleges' && req.method === 'GET') {
        const colleges = readJSON(COLLEGES_FILE, []);
        sendJSON(res, 200, colleges);
        return;
      }

      // 6. POST /api/colleges (Add or Edit Single College)
      if (pathname === '/api/colleges' && req.method === 'POST') {
        const college = await readBody(req);
        let colleges = readJSON(COLLEGES_FILE, []);
        if (college.id) {
          const idx = colleges.findIndex(c => c.id === college.id);
          if (idx !== -1) {
            colleges[idx] = { ...colleges[idx], ...college };
          } else {
            colleges.push(college);
          }
        } else {
          college.id = Date.now();
          colleges.push(college);
        }
        writeJSON(COLLEGES_FILE, colleges);
        sendJSON(res, 200, { success: true, college, total: colleges.length });
        return;
      }

      // 7. POST /api/colleges/batch (Batch Import from Excel)
      if (pathname === '/api/colleges/batch' && req.method === 'POST') {
        const { list, mode } = await readBody(req); // mode: 'replace' | 'append'
        if (!Array.isArray(list)) {
          sendJSON(res, 400, { success: false, message: 'List must be an array of colleges' });
          return;
        }
        let colleges = readJSON(COLLEGES_FILE, []);
        if (mode === 'replace') {
          colleges = list;
        } else {
          // Append or update by code
          list.forEach(item => {
            const existingIdx = colleges.findIndex(c => c.code && item.code && c.code === item.code);
            if (existingIdx !== -1) {
              colleges[existingIdx] = { ...colleges[existingIdx], ...item };
            } else {
              colleges.push(item);
            }
          });
        }
        writeJSON(COLLEGES_FILE, colleges);
        sendJSON(res, 200, { success: true, count: colleges.length, colleges });
        return;
      }

      // 8. DELETE /api/colleges/:id
      if (pathname.startsWith('/api/colleges/') && req.method === 'DELETE') {
        const idToDelete = parseInt(pathname.replace('/api/colleges/', ''));
        let colleges = readJSON(COLLEGES_FILE, []);
        colleges = colleges.filter(c => c.id !== idToDelete);
        writeJSON(COLLEGES_FILE, colleges);
        sendJSON(res, 200, { success: true, remaining: colleges.length });
        return;
      }

      // 9. KMZ Boundary
      if (pathname === '/api/kmz' && req.method === 'GET') {
        const geojson = readJSON(KMZ_FILE, null);
        sendJSON(res, 200, { success: true, geojson });
        return;
      }

      if (pathname === '/api/kmz' && req.method === 'POST') {
        const { geojson } = await readBody(req);
        writeJSON(KMZ_FILE, geojson);
        sendJSON(res, 200, { success: true });
        return;
      }

      if (pathname === '/api/kmz' && req.method === 'DELETE') {
        if (fs.existsSync(KMZ_FILE)) fs.unlinkSync(KMZ_FILE);
        sendJSON(res, 200, { success: true });
        return;
      }

      sendJSON(res, 404, { success: false, message: 'API Endpoint Not Found' });
      return;
    } catch (apiErr) {
      console.error('API Error:', apiErr);
      sendJSON(res, 500, { success: false, error: apiErr.message });
      return;
    }
  }

  // ================= STATIC FILES =================
  if (pathname === '/') {
    pathname = '/index.html';
  }

  const safePath = path.normalize(pathname).replace(/^(\.\.[\/\\])+/, '');
  let filePath = path.join(PUBLIC_DIR, safePath);

  fs.stat(filePath, (err, stats) => {
    if (err || !stats.isFile()) {
      filePath = path.join(PUBLIC_DIR, 'index.html');
    }

    const ext = path.extname(filePath).toLowerCase();
    const contentType = MIME_TYPES[ext] || 'application/octet-stream';

    fs.readFile(filePath, (readErr, content) => {
      if (readErr) {
        res.writeHead(500, { 'Content-Type': 'text/plain' });
        res.end('500 Internal Server Error');
        return;
      }

      res.writeHead(200, {
        'Content-Type': contentType,
        'Cache-Control': 'no-cache, no-store, must-revalidate',
        'Access-Control-Allow-Origin': '*'
      });
      res.end(content);
    });
  });
});

// Crash protection
process.on('uncaughtException', (err) => {
  console.error('Uncaught Exception in server:', err);
});
process.on('unhandledRejection', (reason, promise) => {
  console.error('Unhandled Rejection at:', promise, 'reason:', reason);
});

module.exports = server;

if (!process.env.VERCEL) {
  server.listen(PORT, '0.0.0.0', () => {
    console.log(`UniMap Standalone Web Server running on http://0.0.0.0:${PORT}`);
  });
}
