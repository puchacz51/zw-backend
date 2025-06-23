# Dokumentacja API - Pliki Projektowe

## Przegląd
System umożliwia zarządzanie plikami projektowymi z odpowiednimi walidacjami i uprawnieniami bezpieczeństwa.

## Konfiguracja

### Właściwości aplikacji
```properties
file.project-files-subdir=project-files
file.max-file-size-mb=50
file.max-filename-length=255
spring.servlet.multipart.max-file-size=50MB
spring.servlet.multipart.max-request-size=55MB
```

## Walidacje

### Rozmiar pliku
- Maksymalny rozmiar: 50MB (konfigurowalny)

### Nazwa pliku
- Maksymalna długość: 255 znaków
- Nie może zawierać: `..`, `/`, `\`

### Dozwolone rozszerzenia
- Dokumenty: pdf, doc, docx, xls, xlsx, ppt, pptx, txt, rtf, odt, ods, odp
- Archiwa: zip, rar, 7z
- Obrazy: jpg, jpeg, png, gif, bmp, tiff
- Wideo: mp4, avi, mov, wmv, flv
- Audio: mp3, wav, flac, aac
- Dane: csv, json, xml, sql

### Zabronione rozszerzenia (bezpieczeństwo)
- exe, bat, cmd, com, pif, scr, vbs, js, jar, msi

## Endpointy API

### 1. Upload pliku do projektu
```http
POST /api/projects/{projectId}/files/upload
Content-Type: multipart/form-data
Authorization: Bearer {token}

Parametry:
- file: plik do uploadowania (required)
- description: opis pliku (optional)
```

**Przykład odpowiedzi:**
```json
{
    "id": 1,
    "originalFileName": "dokument.pdf",
    "storedFileName": "uuid-filename.pdf",
    "contentType": "application/pdf",
    "fileSize": 1048576,
    "description": "Ważny dokument projektowy",
    "downloadUrl": "/api/projects/1/files/1/download",
    "uploadedBy": {
        "id": 1,
        "firstName": "Jan",
        "lastName": "Kowalski",
        "email": "jan@example.com",
        "avatarUrl": null
    },
    "uploadDate": "2025-06-23T10:30:00",
    "projectId": 1
}
```

### 2. Lista plików projektu
```http
GET /api/projects/{projectId}/files
Authorization: Bearer {token}
```

### 3. Szczegóły pliku
```http
GET /api/projects/{projectId}/files/{fileId}
Authorization: Bearer {token}
```

### 4. Pobieranie pliku
```http
GET /api/projects/{projectId}/files/{fileId}/download
Authorization: Bearer {token}
```

### 5. Usuwanie pliku
```http
DELETE /api/projects/{projectId}/files/{fileId}
Authorization: Bearer {token}
```

### 6. Pliki użytkownika
```http
GET /api/users/my-files
Authorization: Bearer {token}
```

## Uprawnienia

### Upload plików
- Właściciel projektu
- Użytkownicy przypisani do projektu

### Pobieranie plików
- Właściciel projektu
- Użytkownicy przypisani do projektu

### Usuwanie plików
- Admin (wszystkie pliki)
- Właściciel projektu (wszystkie pliki w projekcie)
- Autor pliku (własne pliki)
- Manager projektu (wszystkie pliki w projekcie)

## Struktura katalogów
```
./uploads/
├── avatars/           # Avatary użytkowników
└── project-files/     # Pliki projektowe
    ├── uuid1.pdf
    ├── uuid2.docx
    └── ...
```

## Przykłady użycia

### Upload pliku (cURL)
```bash
curl -X POST "http://localhost:8080/api/projects/1/files/upload" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -F "file=@document.pdf" \
  -F "description=Specyfikacja techniczna"
```

### Pobierz listę plików projektu
```bash
curl -X GET "http://localhost:8080/api/projects/1/files" \
  -H "Authorization: Bearer YOUR_TOKEN"
```

### Pobierz plik
```bash
curl -X GET "http://localhost:8080/api/projects/1/files/1/download" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -O
```

## Błędy i walidacja

### Najczęstsze błędy
- `400 Bad Request`: Nieprawidłowy plik lub przekroczono limity
- `401 Unauthorized`: Brak autoryzacji
- `403 Forbidden`: Brak uprawnień do operacji
- `404 Not Found`: Projekt lub plik nie istnieje
- `413 Payload Too Large`: Plik zbyt duży

### Przykładowe komunikaty walidacji
- "File size cannot exceed 50MB"
- "Filename cannot exceed 255 characters"
- "File type .exe is not allowed for security reasons"
- "User not authorized to upload files to this project"
