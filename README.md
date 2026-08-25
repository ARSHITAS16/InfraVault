# InfraVault - Password & Infrastructure Inventory System

**InfraVault** is a secure enterprise password management and hardware infrastructure inventory system designed with split-pane navigation, AES-256-GCM secret encryption, datacenter permission delegation, Excel bulk migration, and operational security audit logs.

![RingCentral Logo](frontend/src/assets/ringcentral-logo.png)

---

## ✨ Features

- 🔐 **Encrypted Credentials**: AES-256-GCM encryption for infrastructure secrets (`iDRAC`, `sysadmin`, `secoff`, `passphrase`, `console`). Plaintext is never stored at rest.
- 👁️ **On-Demand Secret Reveal**: One-click password decryption with automatic 15-second masking countdown timer and copy-to-clipboard functionality.
- 🏢 **Datacenter & Tree Navigation**: Left-pane tree view (`Datacenter` → `Folder / Device Group` → `Host`).
- 📋 **Copy Datacenter Permissions**: Easily copy user access mappings from a source Datacenter to a newly provisioned target Datacenter.
- 📊 **Bulk Excel Import (`.xlsx`)**: Upload spreadsheets, preview row validation & duplicate detection, and perform transactional imports.
- 🛡️ **Operational Audit Logs**: Complete security audit trail logging logins, host creation, permission copying, and secret reveal events.

---

## 🛠️ Tech Stack

- **Backend**: Spring Boot 3.3.5, Java 21, Spring Security (JWT Authentication), JPA / Hibernate, BouncyCastle Crypto.
- **Frontend**: React 19, TypeScript, Vite, Lucide Icons, Modern Dark UI.
- **Database**: PostgreSQL 16.

---

## 🚀 Quick Start

### 1. Prerequisites
- **Java 21 JDK** installed
- **Node.js 18+** installed
- **PostgreSQL 16** database running locally (`password_manager` DB)

### 2. Run Spring Boot Backend
```powershell
cd backend
$env:JAVA_HOME="C:\Program Files\Eclipse Adoptium\jdk-21.0.12.8-hotspot"
.\mvnw.cmd clean spring-boot:run
```
*Backend API runs at `http://localhost:8080`*

### 3. Run React Frontend
```powershell
cd frontend
cmd /c "npm install"
cmd /c "npm run dev"
```
*Frontend Web Application runs at `http://localhost:5173` or `http://localhost:5174`*

---

## 🔑 Default Admin Account
- **Username**: `admin`
- **Password**: `password123`
