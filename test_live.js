const https = require('https');

function request(url, options, data) {
  return new Promise((resolve, reject) => {
    const u = new URL(url);
    const reqOptions = {
      hostname: u.hostname,
      port: u.port || 443,
      path: u.pathname + u.search,
      method: options.method || 'GET',
      headers: options.headers || {},
    };
    const req = https.request(reqOptions, (res) => {
      let body = '';
      res.on('data', chunk => body += chunk);
      res.on('end', () => {
        resolve({ statusCode: res.statusCode, headers: res.headers, body });
      });
    });
    req.on('error', reject);
    if (data) req.write(data);
    req.end();
  });
}

async function run() {
  console.log("Logging in...");
  let loginRes = await request(
    'https://infravault-backend-znmg.onrender.com/api/auth/login',
    { method: 'POST', headers: { 'Content-Type': 'application/json' } },
    JSON.stringify({ username: 'admin', password: 'Admin@123' })
  );
  if (loginRes.statusCode !== 200) {
    console.log("Login failed, attempting register...");
    loginRes = await request(
      'https://infravault-backend-znmg.onrender.com/api/auth/register',
      { method: 'POST', headers: { 'Content-Type': 'application/json' } },
      JSON.stringify({ username: 'admin', email: 'admin@infravault.com', password: 'Admin@123', role: 'SUPER_ADMIN' })
    );
  }
  console.log("Auth Status:", loginRes.statusCode, loginRes.body);
  const data = JSON.parse(loginRes.body);
  const token = data.token;

  console.log("\nTesting GET /api/datacenters/1/users...");
  const dcUsersRes = await request(
    'https://infravault-backend-znmg.onrender.com/api/datacenters/1/users',
    { method: 'GET', headers: { 'Authorization': `Bearer ${token}` } }
  );
  console.log("DC Users Status:", dcUsersRes.statusCode, dcUsersRes.body);

  console.log("\nTesting POST /api/credentials/1/update...");
  const updateRes = await request(
    'https://infravault-backend-znmg.onrender.com/api/credentials/1/update',
    { method: 'POST', headers: { 'Authorization': `Bearer ${token}`, 'Content-Type': 'application/json' } },
    JSON.stringify({ newPassword: 'UpdatedSecret123!', datacenterId: 1 })
  );
  console.log("Update Password Status:", updateRes.statusCode, updateRes.body);
}

run().catch(console.error);
