const API_BASE = '/api/notifications';

// Helper for authenticated requests
function getAuthHeaders() {
    const token = sessionStorage.getItem('adminToken');
    return token ? { 'X-Session-Token': token } : {};
}

// Fetch and display notifications on the home page
async function loadNotifications() {
    const list = document.getElementById('notifications-list');
    if (!list) return;

    try {
        const response = await fetch(API_BASE);
        const data = await response.json();

        if (data.length === 0) {
            list.innerHTML = '<p style="text-align: center; color: #666; padding: 20px;">No updates at the moment.</p>';
            return;
        }

        list.innerHTML = data.map(notif => `
            <div class="news-item">
                <div class="news-date">
                    ${new Date(notif.date).toLocaleDateString('en-US', { month: 'short', day: '2-digit' }).toUpperCase()}
                </div>
                <div class="news-content">
                    <h3>${notif.title} <span style="font-size: 0.8rem; background: #eee; padding: 2px 8px; border-radius: 10px; margin-left:10px;">${notif.category}</span></h3>
                    <p>${notif.content}</p>
                </div>
            </div>
        `).join('');
    } catch (error) {
        console.error('Error fetching notifications:', error);
    }
}

async function loadPrincipalHome() {
    const pName = document.getElementById('p-home-name');
    const pImg = document.getElementById('p-home-img');
    const pQuote = document.getElementById('p-home-quote');
    const pPreview = document.getElementById('p-home-msg-preview');

    if (!pName) return;

    try {
        const response = await fetch('/api/principal-message');
        const data = await response.json();
        
        pName.innerText = data.name || "Principal Name";
        pQuote.innerHTML = data.quote ? `"${data.quote}"` : "Education is the manifestation of perfection...";
        pPreview.innerHTML = data.message ? data.message.substring(0, 250).replace(/<[^>]*>?/gm, '') + "..." : "Loading...";
        if (data.photo) pImg.src = data.photo;
    } catch (error) {
        console.log('Principal home skip');
    }
}

// Fetch and display notifications in the admin dashboard
async function loadAdminNotifications() {
    const list = document.getElementById('admin-notifications-list');
    if (!list) return;

    try {
        const response = await fetch(API_BASE);
        const data = await response.json();

        list.innerHTML = data.map(notif => `
            <div class="news-item" style="justify-content: space-between;">
                <div>
                    <strong>${notif.title}</strong> (${notif.category})
                    <p style="font-size: 0.9rem; color: #666;">${notif.content.substring(0, 100)}...</p>
                </div>
                <button onclick="deleteNotification(${notif.id})" style="background: var(--red); color: white; border: none; padding: 5px 12px; border-radius: 5px; cursor: pointer;">Delete</button>
            </div>
        `).join('');
    } catch (error) {
        console.error('Error fetching admin notifications:', error);
    }
}

// Delete notification
async function deleteNotification(id) {
    if (!confirm('Are you sure you want to delete this notification?')) return;

    try {
        await fetch(`${API_BASE}/${id}`, { 
            method: 'DELETE',
            headers: getAuthHeaders()
        });
        loadAdminNotifications();
    } catch (error) {
        console.error('Error deleting notification:', error);
    }
}

// Submit new notification
const notifForm = document.getElementById('notification-form');
if (notifForm) {
    notifForm.addEventListener('submit', async (e) => {
        e.preventDefault();
        
        const payload = {
            title: document.getElementById('notif-title').value,
            category: document.getElementById('notif-category').value,
            content: document.getElementById('notif-content').value
        };

        try {
            const response = await fetch(API_BASE, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json', ...getAuthHeaders() },
                body: JSON.stringify(payload)
            });

            if (response.ok) {
                alert('Notification posted successfully!');
                loadAdminNotifications();
            }
        } catch (error) {
            console.error('Error posting notification:', error);
        }
    });
}

// Submit new review
const reviewForm = document.getElementById('review-form');
if (reviewForm) {
    reviewForm.addEventListener('submit', handleReviewSubmit);
}

// Fetch and display applications in the admin dashboard
async function loadApplications() {
    const list = document.getElementById('applications-list');
    if (!list) return;

    try {
        const response = await fetch('/api/applications', { headers: getAuthHeaders() });
        const data = await response.json();

        if (data.length === 0) {
            list.innerHTML = '<p>No applications received yet.</p>';
            return;
        }

        list.innerHTML = data.map(app => `
            <div class="news-item" style="display: block; background: #f8f9fa; padding: 20px; border-radius: 8px; margin-bottom: 15px;">
                <div style="display: flex; justify-content: space-between; margin-bottom: 10px;">
                    <strong style="font-size: 1.1rem; color: var(--navy);">${app.name}</strong>
                    <span style="color: #666; font-size: 0.8rem;">${new Date(app.timestamp).toLocaleString()}</span>
                </div>
                <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 10px; font-size: 0.9rem;">
                    <div><strong>Class:</strong> ${app.studentClass}</div>
                    <div><strong>Session:</strong> ${app.session}</div>
                    <div><strong>Mobile:</strong> ${app.mobile}</div>
                    <div><strong>Parent:</strong> ${app.fatherName}</div>
                </div>
                <div style="margin-top: 10px; font-size: 0.95rem;"><strong>Gender:</strong> ${app.gender} | <strong>Category:</strong> ${app.category}</div>
            </div>
        `).join('');
    } catch (error) {
        console.error('Error fetching applications:', error);
    }
}

// --- Gallery Logic ---

async function loadGallery() {
    const grid = document.getElementById('gallery-grid');
    if (!grid) return;

    try {
        const response = await fetch('/api/gallery');
        const images = await response.json();

        if (images.length === 0) {
            grid.innerHTML = '<p style="text-align: center; grid-column: 1/-1; color: #666; padding: 40px;">No photos in the gallery yet.</p>';
            return;
        }

        grid.innerHTML = images.map(img => `
            <div class="testimonial-card" style="padding: 10px; padding-top: 10px;">
                <img src="${img.data}" style="width: 100%; height: 200px; object-fit: cover; border-radius: 5px;">
            </div>
        `).join('');
    } catch (error) {
        console.error('Error fetching gallery:', error);
    }
}

async function loadAdminGallery() {
    const list = document.getElementById('admin-gallery-list');
    if (!list) return;

    try {
        const response = await fetch('/api/gallery');
        const images = await response.json();

        list.innerHTML = images.map(img => `
            <div style="border: 1px solid #ddd; padding: 10px; border-radius: 5px; text-align: center;">
                <img src="${img.data}" style="width: 100%; height: 100px; object-fit: cover; border-radius: 3px; margin-bottom: 10px;">
                <button onclick="deleteGalleryImage('${img.id}')" style="background: var(--red); color: white; border: none; padding: 5px 8px; border-radius: 3px; cursor: pointer; font-size: 0.8rem;">Delete</button>
            </div>
        `).join('');
    } catch (error) {
        console.error('Error fetching admin gallery:', error);
    }
}

async function handleGalleryUpload() {
    const fileInput = document.getElementById('gallery-upload');
    if (!fileInput.files[0]) {
        alert('Please select an image first.');
        return;
    }

    const file = fileInput.files[0];
    const reader = new FileReader();

    reader.onload = async (e) => {
        const base64Data = e.target.result;
        const payload = {
            name: `${Date.now()}_${file.name}`,
            data: base64Data
        };

        try {
            const response = await fetch('/api/gallery', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json', ...getAuthHeaders() },
                body: JSON.stringify(payload)
            });

            if (response.ok) {
                alert('Photo uploaded successfully!');
                fileInput.value = '';
                loadAdminGallery();
            }
        } catch (error) {
            console.error('Error uploading photo:', error);
        }
    };
    reader.readAsDataURL(file);
}

async function deleteGalleryImage(fileName) {
    if (!confirm('Delete this photo?')) return;
    try {
        await fetch(`/api/gallery/${fileName}`, { 
            method: 'DELETE',
            headers: getAuthHeaders()
        });
        loadAdminGallery();
    } catch (error) {
        console.error('Error deleting photo:', error);
    }
}

async function loadCareers() {
    const list = document.getElementById('careers-list');
    if (!list) return;

    try {
        const response = await fetch('/api/careers', { headers: getAuthHeaders() });
        const data = await response.json();

        if (data.length === 0) {
            list.innerHTML = '<p>No career applications received yet.</p>';
            return;
        }

        list.innerHTML = data.map(app => `
            <div class="news-item" style="display: block; background: #fffaf0; padding: 20px; border-radius: 8px; margin-bottom: 15px; border-left: 5px solid var(--navy);">
                <div style="display: flex; justify-content: space-between; margin-bottom: 10px;">
                    <strong style="font-size: 1.1rem; color: var(--navy);">${app.name}</strong>
                    <span style="color: #666; font-size: 0.8rem;">${new Date(app.timestamp).toLocaleString()}</span>
                </div>
                <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 10px; font-size: 0.9rem;">
                    <div><strong>Profile:</strong> ${app.profile}</div>
                    <div><strong>Experience:</strong> ${app.experience}</div>
                    <div><strong>Mobile:</strong> ${app.mobile}</div>
                    <div><strong>Email:</strong> ${app.email}</div>
                </div>
            </div>
        `).join('');
    } catch (error) {
        console.error('Error fetching careers:', error);
    }
}

async function loadVisionAdmin() {
    const input = document.getElementById('vision-msg-input');
    if (!input) return;

    try {
        const response = await fetch('/api/vision-mission', { headers: getAuthHeaders() });
        const data = await response.json();
        input.value = data.content || "";
    } catch (error) {
        console.error('Error fetching vision:', error);
    }
}

async function updateVisionMission() {
    const msg = document.getElementById('vision-msg-input').value;
    try {
        const response = await fetch('/api/vision-mission', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json', ...getAuthHeaders() },
            body: JSON.stringify({ content: msg })
        });

        if (response.ok) {
            alert('Vision & Mission updated successfully!');
        }
    } catch (error) {
        console.error('Error updating vision:', error);
    }
}

async function loadPrincipalMsgAdmin() {
    const nameInput = document.getElementById('principal-name-input');
    const quoteInput = document.getElementById('principal-quote-input');
    const msgInput = document.getElementById('principal-msg-input');
    const photoV = document.getElementById('p-photo-v');
    const sealV = document.getElementById('p-seal-v');
    const signV = document.getElementById('p-sign-v');

    if (!msgInput) return;

    try {
        const response = await fetch('/api/principal-message');
        const data = await response.json();
        
        if (nameInput) nameInput.value = data.name || "";
        if (quoteInput) quoteInput.value = data.quote || "";
        if (msgInput) msgInput.value = data.message || "";
        if (photoV && data.photo) {
            photoV.src = data.photo;
            photoV.style.display = 'block';
        }
        if (sealV && data.seal) {
            sealV.src = data.seal;
            sealV.style.display = 'block';
        }
        if (signV && data.signature) {
            signV.src = data.signature;
            signV.style.display = 'block';
        }
    } catch (error) {
        console.error('Error fetching principal message:', error);
    }
}

async function updatePrincipalProfile() {
    const name = document.getElementById('principal-name-input').value;
    const quote = document.getElementById('principal-quote-input').value;
    const message = document.getElementById('principal-msg-input').value;
    const photoInput = document.getElementById('principal-photo-input');
    const sealInput = document.getElementById('principal-seal-input');
    const signInput = document.getElementById('principal-sign-input');

    const updateData = { name, quote, message, photo: "", seal: "", signature: "" };

    const readFile = (file) => {
        return new Promise((resolve) => {
            if (!file) resolve("");
            const reader = new FileReader();
            reader.onload = (e) => resolve(e.target.result);
            reader.readAsDataURL(file);
        });
    };

    try {
        updateData.photo = await readFile(photoInput.files[0]);
        updateData.seal = await readFile(sealInput.files[0]);
        updateData.signature = await readFile(signInput.files[0]);

        const response = await fetch('/api/principal-message', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json', ...getAuthHeaders() },
            body: JSON.stringify(updateData)
        });

        if (response.ok) {
            alert('Principal profile and branding updated successfully!');
            loadPrincipalMsgAdmin();
        }
    } catch (error) {
        console.error('Error updating principal:', error);
    }
}

// --- Testimonial Management ---

async function loadTestimonials() {
    const grid = document.getElementById('testimonials-live-grid');
    if (!grid) return;

    try {
        const response = await fetch('/api/testimonials');
        const data = await response.json();

        if (data.length === 0) {
            grid.innerHTML = '<p style="color: white; text-align: center; grid-column: 1/-1;">Be the first to share your feedback!</p>';
            return;
        }

        grid.innerHTML = data.map(t => `
            <div class="testimonial-card">
                <div class="quote-icon"><i class="fas fa-quote-left"></i></div>
                <p>"${t.message}"</p>
                <div style="margin-top: 20px; display: flex; align-items: center; gap: 15px;">
                    <div>
                        <strong>${t.name}</strong><br>
                        <small style="color: var(--yellow);">${t.relation}</small>
                    </div>
                </div>
            </div>
        `).join('');
    } catch (error) {
        console.error('Error loading testimonials:', error);
    }
}

async function handleReviewSubmit(e) {
    e.preventDefault();
    const btn = e.target.querySelector('button');
    btn.disabled = true;

    const review = {
        name: document.getElementById('review-name').value,
        relation: document.getElementById('review-relation').value,
        message: document.getElementById('review-message').value,
        timestamp: new Date().toISOString()
    };

    try {
        const response = await fetch('/api/testimonials', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(review)
        });

        if (response.ok) {
            alert('Thank you for your feedback! Your review has been submitted.');
            e.target.reset();
            loadTestimonials();
        }
    } catch (error) {
        console.error('Error submitting review:', error);
    } finally {
        btn.disabled = false;
    }
}

async function loadTestimonialsAdmin() {
    const list = document.getElementById('testimonials-admin-list');
    if (!list) return;

    try {
        const response = await fetch('/api/testimonials');
        const data = await response.json();

        list.innerHTML = data.map(t => `
            <div class="admin-item-card" id="t-card-${t.id}">
                <div style="display: flex; justify-content: space-between; margin-bottom: 15px;">
                    <div>
                        <strong style="color: var(--navy); font-size: 1.1rem;">${t.name}</strong>
                        <div style="color: #666; font-size: 0.8rem;">${t.relation} | ${new Date(t.timestamp).toLocaleDateString()}</div>
                    </div>
                    <div style="display: flex; gap: 10px;">
                        <button onclick="editTestimonial('${t.id}')" class="primary" style="background: var(--navy); padding: 5px 15px;">Edit</button>
                        <button onclick="deleteTestimonial('${t.id}')" class="primary" style="background: var(--red); padding: 5px 15px;">Delete</button>
                    </div>
                </div>
                <div id="t-text-${t.id}" style="font-style: italic; color: #4a5568;">"${t.message}"</div>
            </div>
        `).join('');
    } catch (error) {
        console.log('Error loading admin testimonials');
    }
}

// --- Faculty Management ---

async function loadFaculty() {
    const grid = document.getElementById('faculty-live-grid');
    if (!grid) return;

    try {
        const response = await fetch('/api/faculty');
        const data = await response.json();

        if (data.length === 0) {
            grid.innerHTML = '<p style="text-align: center; grid-column: 1/-1; color: #666; padding: 40px;">Faculty profiles will appear here soon.</p>';
            return;
        }

        grid.innerHTML = data.map(f => `
            <div class="faculty-card">
                <img src="${f.photo}" alt="${f.name}" class="faculty-img">
                <div class="faculty-name">${f.name}</div>
                <div style="color: #666; font-size: 0.8rem;">Dedicated Teacher</div>
            </div>
        `).join('');
    } catch (error) {
        console.error('Error loading faculty:', error);
    }
}

async function loadFacultyAdmin() {
    const list = document.getElementById('faculty-admin-list');
    if (!list) return;

    try {
        const response = await fetch('/api/faculty');
        const data = await response.json();

        list.innerHTML = data.map(f => `
            <div style="border: 1px solid #ddd; padding: 15px; border-radius: 10px; text-align: center; background: #fff;">
                <img src="${f.photo}" style="width: 80px; height: 80px; border-radius: 50%; object-fit: cover; margin-bottom: 10px;">
                <div style="font-weight: 600; margin-bottom: 10px; font-size: 0.9rem;">${f.name}</div>
                <button onclick="deleteFaculty('${f.id}')" style="background: var(--red); color: white; border: none; padding: 5px 10px; border-radius: 5px; cursor: pointer; font-size: 0.75rem;">Remove</button>
            </div>
        `).join('');
    } catch (error) {
        console.error('Error loading admin faculty');
    }
}

async function addFaculty() {
    const nameInput = document.getElementById('faculty-name-input');
    const photoInput = document.getElementById('faculty-photo-input');

    if (!nameInput.value || !photoInput.files[0]) {
        alert('Please provide both name and photo.');
        return;
    }

    const reader = new FileReader();
    reader.onload = async (e) => {
        const payload = {
            name: nameInput.value,
            photo: e.target.result
        };

        try {
            const response = await fetch('/api/faculty', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json', ...getAuthHeaders() },
                body: JSON.stringify(payload)
            });

            if (response.ok) {
                alert('Teacher profile added successfully!');
                nameInput.value = '';
                photoInput.value = '';
                loadFacultyAdmin();
            }
        } catch (error) {
            console.error('Error adding faculty:', error);
        }
    };
    reader.readAsDataURL(photoInput.files[0]);
}

async function deleteFaculty(id) {
    if (!confirm('Remove this teacher profile?')) return;
    try {
        const response = await fetch(`/api/faculty/${id}`, { 
            method: 'DELETE',
            headers: getAuthHeaders()
        });
        if (response.ok) loadFacultyAdmin();
    } catch (error) { console.error(error); }
}

async function deleteTestimonial(id) {
    if (!confirm('Are you sure you want to delete this testimonial?')) return;
    try {
        const response = await fetch(`/api/testimonials/${id}`, { 
            method: 'DELETE',
            headers: getAuthHeaders()
        });
        if (response.ok) loadTestimonialsAdmin();
    } catch (error) { console.error(error); }
}

async function editTestimonial(id) {
    const card = document.getElementById(`t-card-${id}`);
    const textDiv = document.getElementById(`t-text-${id}`);
    const originalText = textDiv.innerText.replace(/"/g, '');
    
    textDiv.innerHTML = `
        <textarea id="edit-area-${id}" style="width: 100%; padding: 10px; margin-bottom: 10px;" rows="3">${originalText}</textarea>
        <button onclick="saveTestimonialEdit('${id}')" class="primary" style="background: #2f855a;">Save Changes</button>
        <button onclick="loadTestimonialsAdmin()" class="primary" style="background: #a0aec0; margin-left: 10px;">Cancel</button>
    `;
}

async function saveTestimonialEdit(id) {
    const newMsg = document.getElementById(`edit-area-${id}`).value;
    
    // First get current data to keep other fields
    const res = await fetch('/api/testimonials');
    const data = await res.json();
    const originalEntry = data.find(t => t.id === id);
    
    const updatedEntry = { ...originalEntry, message: newMsg };

    try {
        const response = await fetch('/api/testimonials', {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json', ...getAuthHeaders() },
            body: JSON.stringify(updatedEntry)
        });

        if (response.ok) {
            alert('Review updated successfully!');
            loadTestimonialsAdmin();
        }
    } catch (error) { console.error(error); }
}

// Update showSection to include testimonials
function showSection(sectionId) {
    const sections = ['notifications-dash', 'applications-dash', 'gallery-dash', 'careers-dash', 'principal-dash', 'testimonials-dash', 'vision-dash', 'faculty-dash', 'students-dash', 'results-dash', 'settings-dash', 'library-dash', 'enquiries-dash'];
    
    // Hide all sections
    sections.forEach(id => {
        const el = document.getElementById(id);
        if (el) el.style.display = id === sectionId ? 'block' : 'none';
        
        // Handle Sidebar Active States
        const navId = 'nav-' + id.split('-')[0];
        const navEl = document.getElementById(navId);
        if (navEl) {
            if (id === sectionId) navEl.classList.add('active');
            else navEl.classList.remove('active');
        }
    });

    // Update Top Bar Title
    const titleMap = {
        'notifications-dash': 'Manage News',
        'applications-dash': 'Admission Applications',
        'gallery-dash': 'Photo Gallery',
        'careers-dash': 'Career Applications',
        'principal-dash': 'Principal Profile',
        'testimonials-dash': 'Parent Testimonials',
        'vision-dash': 'Vision & Mission',
        'faculty-dash': 'Manage Faculty',
        'students-dash': 'Student ID Generator',
        'results-dash': 'Manage Student Results',
        'settings-dash': 'School Settings',
        'library-dash': 'E-Library Management',
        'enquiries-dash': 'Website Enquiries'
    };
    const titleEl = document.getElementById('current-section-title');
    if (titleEl && titleMap[sectionId]) titleEl.innerText = titleMap[sectionId];
    
    // Load data for specific sections
    if (sectionId === 'applications-dash') loadApplications();
    if (sectionId === 'gallery-dash') loadAdminGallery();
    if (sectionId === 'careers-dash') loadCareers();
    if (sectionId === 'principal-dash') loadPrincipalMsgAdmin();
    if (sectionId === 'testimonials-dash') loadTestimonialsAdmin();
    if (sectionId === 'vision-dash') loadVisionAdmin();
    if (sectionId === 'faculty-dash') loadFacultyAdmin();
    if (sectionId === 'students-dash') loadStudents();
    if (sectionId === 'results-dash') loadResults();
    if (sectionId === 'settings-dash') loadSchoolSettings();
    if (sectionId === 'library-dash') loadAdminLibrary();
    if (sectionId === 'enquiries-dash') loadEnquiries();
}

// Update Initialize
document.addEventListener('DOMContentLoaded', () => {
    dynamicizeContactInfo();
    loadNotifications();
    loadGallery();
    loadTestimonials();
    loadFaculty();
    loadPrincipalHome();
    initEnquiryForm();
    if (sessionStorage.getItem('adminToken')) {
        const sections = ['notifications-dash', 'applications-dash', 'gallery-dash', 'careers-dash', 'principal-dash', 'testimonials-dash', 'vision-dash', 'faculty-dash', 'students-dash', 'results-dash', 'settings-dash', 'library-dash', 'enquiries-dash'];
        const activeSection = sections.find(id => {
            const el = document.getElementById(id);
            return el && el.style.display === 'block';
        }) || 'notifications-dash';
        showSection(activeSection);
    }
});

// --- Student ID Card Management ---
let currentStudents = []; // Global state to avoid JSON stringify in HTML

async function loadStudents() {
    const list = document.getElementById('admin-students-list');
    if (!list) return;

    try {
        const response = await fetch('/api/students', { headers: getAuthHeaders() });
        currentStudents = await response.json();
        list.innerHTML = '';
        
        currentStudents.forEach((s, index) => {
            const card = document.createElement('div');
            card.className = 'admin-card';
            card.style.padding = '15px';
            card.style.background = 'white';
            card.style.border = '1px solid #e2e8f0';
            card.style.borderRadius = '10px';
            card.innerHTML = `
                <div style="display: flex; gap: 15px; align-items: start;">
                    <img src="${s.photo}" style="width: 70px; height: 70px; border-radius: 8px; object-fit: cover; border: 2px solid #ddd;">
                    <div style="flex: 1;">
                        <h4 style="margin: 0; color: var(--navy);">${s.name}</h4>
                        <p style="font-size: 0.8rem; color: #666; margin: 3px 0;">Class: ${s.student_class} | Roll: ${s.roll_no}</p>
                        <div style="display: flex; gap: 8px; margin-top: 10px;">
                            <button onclick="openIDCardPreview(${index})" class="primary" style="padding: 5px 12px; font-size: 0.75rem; background: #27ae60; cursor: pointer;">
                                <i class="fas fa-eye"></i> View ID
                            </button>
                            <button onclick="deleteStudent(${s.id})" style="padding: 5px 12px; font-size: 0.75rem; background: none; border: 1px solid #ff4d4d; color: #ff4d4d; border-radius: 5px; cursor: pointer;">
                                <i class="fas fa-trash"></i>
                            </button>
                        </div>
                    </div>
                </div>
            `;
            list.appendChild(card);
        });
    } catch (e) { console.error('Error loading students:', e); }
}

const studentForm = document.getElementById('student-form');
if (studentForm) {
    studentForm.addEventListener('submit', async (e) => {
        e.preventDefault();
        const photoFile = document.getElementById('stu-photo').files[0];
        let photoBase64 = '';

        if (photoFile) {
            const reader = new FileReader();
            photoBase64 = await new Promise(resolve => {
                reader.onload = (e) => resolve(e.target.result);
                reader.readAsDataURL(photoFile);
            });
        }

        const data = {
            name: document.getElementById('stu-name').value,
            father_name: document.getElementById('stu-father').value,
            student_class: document.getElementById('stu-class').value,
            roll_no: document.getElementById('stu-roll').value,
            dob: document.getElementById('stu-dob').value,
            blood_group: document.getElementById('stu-blood').value,
            address: document.getElementById('stu-address').value,
            photo: photoBase64
        };

        try {
            await fetch('/api/students', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json', ...getAuthHeaders() },
                body: JSON.stringify(data)
            });
            studentForm.reset();
            document.getElementById('add-student-form-wrap').style.display = 'none';
            loadStudents();
        } catch (e) { alert('Error saving student'); }
    });
}

async function deleteStudent(id) {
    if (confirm('Delete this student record?')) {
        await fetch(`/api/students/${id}`, { 
            method: 'DELETE',
            headers: getAuthHeaders()
        });
        loadStudents();
    }
}

async function openIDCardPreview(index) {
    const s = currentStudents[index];
    if (!s) return;
    
    document.getElementById('card-name').textContent = s.name;
    document.getElementById('card-father').textContent = s.father_name;
    document.getElementById('card-class').textContent = s.student_class;
    document.getElementById('card-roll').textContent = s.roll_no;
    document.getElementById('card-dob').textContent = s.dob;
    document.getElementById('card-blood').textContent = s.blood_group || '-';
    document.getElementById('card-address').textContent = s.address;
    document.getElementById('card-photo').src = s.photo;
    
    // Load official signature
    try {
        const resp = await fetch('/api/principal-message');
        const data = await resp.json();
        if (data.signature) {
            document.getElementById('card-sign').src = data.signature;
            document.getElementById('card-sign').style.opacity = '1';
        }
    } catch(e) {}

    document.getElementById('id-card-modal').style.display = 'flex';
}

function downloadIDCard() {
    const element = document.getElementById('id-card-capture-area');
    const studentName = document.getElementById('card-name').textContent;
    
    html2canvas(element, {
        scale: 4, // Higher quality
        useCORS: true,
        backgroundColor: null
    }).then(canvas => {
        const link = document.createElement('a');
        link.download = `ID_Card_${studentName.replace(/\s+/g, '_')}.png`;
        link.href = canvas.toDataURL('image/png');
    });
}

// --- Student Result Management ---
function addSubjectInput() {
    const container = document.getElementById('subject-inputs');
    const div = document.createElement('div');
    div.style.display = 'flex';
    div.style.gap = '10px';
    div.style.marginBottom = '10px';
    div.innerHTML = `
        <input type="text" placeholder="Subject Name" class="sub-name" style="flex: 1; padding: 10px; border: 1px solid #ddd; border-radius: 5px;">
        <input type="number" placeholder="Marks" class="sub-marks" style="width: 100px; padding: 10px; border: 1px solid #ddd; border-radius: 5px;">
        <button type="button" onclick="this.parentElement.remove()" style="padding: 10px; background: #fee2e2; color: #991b1b; border: none; border-radius: 5px; cursor: pointer;"><i class="fas fa-trash"></i></button>
    `;
    container.appendChild(div);
}

async function loadResults() {
    const table = document.getElementById('admin-results-table');
    if (!table) return;

    try {
        const response = await fetch('/api/results', { headers: getAuthHeaders() });
        const results = await response.json();
        table.innerHTML = '';
        
        results.forEach(r => {
            const tr = document.createElement('tr');
            tr.innerHTML = `
                <td style="padding: 15px; border-bottom: 1px solid #eee;">${r.roll_no}</td>
                <td style="padding: 15px; border-bottom: 1px solid #eee;">${r.student_name}</td>
                <td style="padding: 15px; border-bottom: 1px solid #eee;">${r.student_class}</td>
                <td style="padding: 15px; border-bottom: 1px solid #eee;">${r.obtained}/${r.total}</td>
                <td style="padding: 15px; border-bottom: 1px solid #eee;">${r.percentage}%</td>
                <td style="padding: 15px; border-bottom: 1px solid #eee;"><span class="status-badge ${r.status.toLowerCase()}">${r.status}</span></td>
                <td style="padding: 15px; border-bottom: 1px solid #eee;">
                    <button onclick="deleteResult(${r.id})" style="padding: 5px 10px; background: none; border: 1px solid #ff4d4d; color: #ff4d4d; border-radius: 3px; cursor: pointer;"><i class="fas fa-trash"></i></button>
                </td>
            `;
            table.appendChild(tr);
        });
    } catch (e) { console.error('Error loading results:', e); }
}

const resultForm = document.getElementById('result-form');
if (resultForm) {
    resultForm.addEventListener('submit', async (e) => {
        e.preventDefault();
        
        const subjects = {};
        const subNames = document.querySelectorAll('.sub-name');
        const subMarks = document.querySelectorAll('.sub-marks');
        subNames.forEach((n, i) => {
            if (n.value && subMarks[i].value) {
                subjects[n.value] = subMarks[i].value;
            }
        });

        const data = {
            roll_no: document.getElementById('res-roll').value,
            student_name: document.getElementById('res-name').value,
            student_class: document.getElementById('res-class').value,
            total_marks: document.getElementById('res-total').value,
            obtained_marks: document.getElementById('res-obtained').value,
            subjects: JSON.stringify(subjects)
        };

        try {
            const resp = await fetch('/api/results', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json', ...getAuthHeaders() },
                body: JSON.stringify(data)
            });
            if (resp.ok) {
                resultForm.reset();
                document.getElementById('add-result-form-wrap').style.display = 'none';
                loadResults();
                alert('Result saved successfully!');
            }
        } catch (e) { alert('Error saving result'); }
    });
}

async function deleteResult(id) {
    if (confirm('Delete this result record?')) {
        await fetch(`/api/results/${id}`, { 
            method: 'DELETE',
            headers: getAuthHeaders()
        });
        loadResults();
    }
}

// --- Excel Bulk Upload Logic ---
function downloadExcelTemplate() {
    const data = [
        ["RollNo", "StudentName", "Class", "TotalMarks", "ObtainedMarks", "Math", "Science", "English", "Hindi", "SocialStudies"],
        ["101", "John Doe", "10th", 500, 420, 85, 80, 75, 90, 90],
        ["102", "Jane Smith", "10th", 500, 450, 95, 90, 85, 90, 90]
    ];
    const ws = XLSX.utils.aoa_to_sheet(data);
    const wb = XLSX.utils.book_new();
    XLSX.utils.book_append_sheet(wb, ws, "Results");
    XLSX.writeFile(wb, "Student_Result_Template.xlsx");
}

async function handleExcelUpload(input) {
    const file = input.files[0];
    if (!file) return;

    const reader = new FileReader();
    reader.onload = async (e) => {
        try {
            const data = new Uint8Array(e.target.result);
            const workbook = XLSX.read(data, { type: 'array' });
            const sheetName = workbook.SheetNames[0];
            const rows = XLSX.utils.sheet_to_json(workbook.Sheets[sheetName]);

            let successCount = 0;
            for (const row of rows) {
                const subjects = {};
                let calcObtained = 0;
                
                // Flexible column mapping
                const roll = row.RollNo || row.roll_no || row['Roll No'] || "";
                const name = row.StudentName || row.student_name || row['Student Name'] || row.name || "";
                const sClass = row.Class || row.student_class || row.class || "";
                let total = parseInt(row.TotalMarks || row.total_marks || row.total || 0);
                let obtained = parseInt(row.ObtainedMarks || row.obtained_marks || row.obtained || 0);

                // Identify subjects: everything else is a subject
                const standardKeys = ['rollno', 'roll_no', 'roll no', 'studentname', 'student name', 'student_name', 'name', 'class', 'student_class', 'totalmarks', 'total_marks', 'total', 'obtainedmarks', 'obtained_marks', 'obtained', 'percentage', 'status', 'grade'];
                
                Object.keys(row).forEach(key => {
                    const k = key.trim().toLowerCase();
                    if (!standardKeys.includes(k)) {
                        const marks = parseInt(row[key]) || 0;
                        subjects[key] = marks;
                        calcObtained += marks;
                    }
                });

                // Auto-calculate if data missing
                if (obtained === 0) obtained = calcObtained;
                if (total === 0) total = Object.keys(subjects).length * 100;

                const payload = {
                    roll_no: roll.toString(),
                    student_name: name,
                    student_class: sClass,
                    total_marks: total.toString(),
                    obtained_marks: obtained.toString(),
                    subjects: JSON.stringify(subjects)
                };

                if (!payload.roll_no || !payload.student_name) continue;

                const resp = await fetch('/api/results', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json', ...getAuthHeaders() },
                    body: JSON.stringify(payload)
                });
                if (resp.ok) successCount++;
            }

            alert(`Successfully uploaded ${successCount} results!`);
            loadResults();
            input.value = '';
        } catch (error) {
            console.error(error);
            alert('Error parsing Excel file. Please ensure it is a valid Excel format.');
        }
    };
    reader.readAsArrayBuffer(file);
}
// --- School Settings Management ---
async function loadSchoolSettings() {
    try {
        const response = await fetch('/api/settings', { headers: getAuthHeaders() });
        const data = await response.json();
        
        const emailInput = document.getElementById('set-email');
        const phoneInput = document.getElementById('set-phone');
        const addressInput = document.getElementById('set-address');
        const mapsInput = document.getElementById('set-maps');

        if (emailInput) emailInput.value = data.email || '';
        if (phoneInput) phoneInput.value = data.phone || '';
        if (addressInput) addressInput.value = data.address || '';
        if (mapsInput) mapsInput.value = data.maps_link || '';
    } catch (e) { console.error("Settings load failed", e); }
}

async function updateSchoolSettings() {
    const data = {
        email: document.getElementById('set-email').value,
        phone: document.getElementById('set-phone').value,
        address: document.getElementById('set-address').value,
        maps_link: document.getElementById('set-maps').value
    };

    try {
        const response = await fetch('/api/settings', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json', ...getAuthHeaders() },
            body: JSON.stringify(data)
        });
        if (response.ok) {
            alert('Settings updated successfully!');
            dynamicizeContactInfo(); // Update current page
        }
    } catch (e) { alert('Error updating settings'); }
}

async function dynamicizeContactInfo() {
    try {
        const response = await fetch('/api/settings');
        const data = await response.json();

        // Update Header Phone
        const headerPhone = document.querySelector('.school-info span:nth-child(2)');
        if (headerPhone) {
            headerPhone.innerHTML = `<i class="fas fa-phone"></i> <a href="tel:${data.phone}" style="color: inherit; text-decoration: none;">${data.phone}</a>`;
        }

        // Update Header Map
        const headerMap = document.querySelector('.school-info span:nth-child(1)');
        if (headerMap) {
           headerMap.innerHTML = `<i class="fas fa-map-marker-alt"></i> <a href="${data.maps_link}" target="_blank" style="color: inherit; text-decoration: none;">${data.address.split(',')[0]}...</a>`;
        }

        // Update Footer Details (targeting by icon to be safe)
        const footerCol = document.querySelector('.footer-col');
        if (footerCol) {
            const paragraphs = footerCol.querySelectorAll('p');
            paragraphs.forEach(p => {
                if (p.innerHTML.includes('fa-location-dot') || p.innerHTML.includes('fa-map-marker-alt')) {
                    p.innerHTML = `<a href="${data.maps_link}" target="_blank" style="color: white; text-decoration: none;"><i class="fas fa-location-dot"></i> ${data.address}</a>`;
                } else if (p.innerHTML.includes('fa-phone')) {
                    p.innerHTML = `<i class="fas fa-phone"></i> <a href="tel:${data.phone}" style="color: white; text-decoration: none;">${data.phone}</a>`;
                } else if (p.innerHTML.includes('fa-envelope')) {
                    p.innerHTML = `<i class="fas fa-envelope"></i> <a href="mailto:${data.email}" style="color: white; text-decoration: none;">${data.email}</a>`;
                }
            });
        }

    } catch (e) { console.warn("Dynamic contact population failed", e); }
}

// --- E-Library Management ---

async function loadAdminLibrary() {
    const table = document.getElementById('admin-library-table');
    if (!table) return;

    try {
        const response = await fetch('/api/library');
        const books = await response.json();
        table.innerHTML = '';
        
        books.forEach(book => {
            const tr = document.createElement('tr');
            tr.innerHTML = `
                <td style="padding: 15px; border-bottom: 1px solid #eee;"><strong>${book.title}</strong></td>
                <td style="padding: 15px; border-bottom: 1px solid #eee;">${book.author}</td>
                <td style="padding: 15px; border-bottom: 1px solid #eee;">${new Date(book.date).toLocaleDateString()}</td>
                <td style="padding: 15px; border-bottom: 1px solid #eee;">
                    <a href="books/${book.pdf_path}" target="_blank" style="color: var(--navy); margin-right: 15px;"><i class="fas fa-eye"></i> View</a>
                    <button onclick="deleteBook(${book.id})" style="padding: 5px 10px; background: none; border: 1px solid #ff4d4d; color: #ff4d4d; border-radius: 3px; cursor: pointer;"><i class="fas fa-trash"></i></button>
                </td>
            `;
            table.appendChild(tr);
        });
    } catch (e) { console.error('Error loading library:', e); }
}

const bookForm = document.getElementById('book-form');
if (bookForm) {
    bookForm.addEventListener('submit', async (e) => {
        e.preventDefault();
        const pdfFile = document.getElementById('lib-pdf').files[0];
        if (!pdfFile) return;

        const reader = new FileReader();
        reader.onload = async (event) => {
            const data = {
                title: document.getElementById('lib-title').value,
                author: document.getElementById('lib-author').value,
                data: event.target.result
            };

            try {
                const resp = await fetch('/api/library', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json', ...getAuthHeaders() },
                    body: JSON.stringify(data)
                });
                if (resp.ok) {
                    bookForm.reset();
                    document.getElementById('add-book-form-wrap').style.display = 'none';
                    loadAdminLibrary();
                    alert('Book uploaded successfully!');
                }
            } catch (e) { alert('Error uploading book'); }
        };
        reader.readAsDataURL(pdfFile);
    });
}

async function deleteBook(id) {
    if (confirm('Delete this book from the library?')) {
        await fetch(`/api/library/${id}`, { 
            method: 'DELETE',
            headers: getAuthHeaders()
        });
        loadAdminLibrary();
    }
}

async function loadStudentLibrary() {
    const list = document.getElementById('student-book-list');
    if (!list) return;

    try {
        const response = await fetch('/api/library');
        const books = await response.json();
        
        if (books.length === 0) {
            list.innerHTML = '<p style="text-align: center; grid-column: 1/-1; color: #666; padding: 40px;">No books available in the library yet.</p>';
            return;
        }

        list.innerHTML = books.map(book => `
            <div class="book-card">
                <div class="book-icon"><i class="fas fa-file-pdf"></i></div>
                <div class="book-title">${book.title}</div>
                <div class="book-author">By ${book.author}</div>
                <div class="book-actions">
                    <a href="books/${book.pdf_path}" target="_blank" class="book-btn btn-read">READ ONLINE</a>
                    <a href="books/${book.pdf_path}" download="${book.title}.pdf" class="book-btn btn-download">DOWNLOAD</a>
                </div>
            </div>
        `).join('');
    } catch (error) {
        console.error('Error fetching library:', error);
    }
}

// --- Enquiry Management ---

function initEnquiryForm() {
    const form = document.getElementById('enquiry-form');
    if (!form) return;

    form.addEventListener('submit', async (e) => {
        e.preventDefault();
        const btn = form.querySelector('button');
        btn.disabled = true;
        btn.innerText = 'Sending...';

        const data = {
            name: document.getElementById('enq-name').value,
            email: document.getElementById('enq-email').value,
            mobile: document.getElementById('enq-mobile').value,
            subject: document.getElementById('enq-subject').value,
            message: document.getElementById('enq-message').value
        };

        try {
            const resp = await fetch('/api/enquiries', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(data)
            });

            if (resp.ok) {
                alert('Success! Your enquiry has been sent to the school administration.');
                form.reset();
            } else {
                alert('Failed to send enquiry. Please try again later.');
            }
        } catch (err) {
            console.error(err);
            alert('Connection error. Please check your internet.');
        } finally {
            btn.disabled = false;
            btn.innerText = 'Send Message';
        }
    });
}

async function loadEnquiries() {
    const table = document.getElementById('admin-enquiries-table');
    if (!table) return;

    try {
        const response = await fetch('/api/enquiries', { headers: getAuthHeaders() });
        const data = await response.json();
        
        if (data.length === 0) {
            table.innerHTML = '<tr><td colspan="5" style="padding: 30px; text-align: center; color: #666;">No enquiries received yet.</td></tr>';
            return;
        }

        table.innerHTML = data.map(enq => `
            <tr>
                <td style="padding: 15px; border-bottom: 1px solid #eee; font-size: 0.85rem; color: #666;">
                    ${new Date(enq.timestamp).toLocaleString()}
                </td>
                <td style="padding: 15px; border-bottom: 1px solid #eee;">
                    <strong>${enq.name}</strong><br>
                    <small>${enq.mobile} | ${enq.email}</small>
                </td>
                <td style="padding: 15px; border-bottom: 1px solid #eee; color: var(--navy); font-weight: 500;">
                    ${enq.subject}
                </td>
                <td style="padding: 15px; border-bottom: 1px solid #eee; font-size: 0.9rem; max-width: 300px; overflow: hidden; text-overflow: ellipsis;">
                    ${enq.message}
                </td>
                <td style="padding: 15px; border-bottom: 1px solid #eee;">
                    <button onclick="deleteEnquiry(${enq.id})" style="padding: 5px 10px; background: none; border: 1px solid #ff4d4d; color: #ff4d4d; border-radius: 3px; cursor: pointer;">
                        <i class="fas fa-trash"></i>
                    </button>
                </td>
            </tr>
        `).join('');
    } catch (e) {
        console.error('Error loading enquiries:', e);
    }
}

async function deleteEnquiry(id) {
    if (!confirm('Are you sure you want to delete this enquiry?')) return;
    try {
        const resp = await fetch(`/api/enquiries/${id}`, { 
            method: 'DELETE',
            headers: getAuthHeaders()
        });
        if (resp.ok) loadEnquiries();
    } catch (e) { console.error(e); }
}
