import { useState, useEffect } from 'react';
import { FiMail, FiPhone, FiMapPin, FiGithub, FiLinkedin, FiInstagram, FiDownload, FiArrowRight, FiCode, FiBriefcase, FiExternalLink, FiSend } from 'react-icons/fi';
import { profileAPI, projectsAPI, skillsAPI, contactsAPI, analyticsAPI } from '../api';
import Navbar from '../components/Navbar';
import Footer from '../components/Footer';
import ParticlesBg from '../components/ParticlesBg';
import { motion } from 'framer-motion';

export default function Portfolio() {
  const [profile, setProfile] = useState(null);
  const [projects, setProjects] = useState([]);
  const [skills, setSkills] = useState([]);
  const [loading, setLoading] = useState(true);
  const [contactForm, setContactForm] = useState({ name: '', email: '', subject: '', message: '' });
  const [formStatus, setFormStatus] = useState(null);
  const [sending, setSending] = useState(false);

  useEffect(() => {
    loadData();
    analyticsAPI.recordVisit();

    // Auto-refresh dynamically when user returns to the portfolio tab
    const handleFocus = () => loadData(true);
    const handleVisibilityChange = () => {
      if (document.visibilityState === 'visible') {
        loadData(true);
      }
    };
    window.addEventListener('focus', handleFocus);
    document.addEventListener('visibilitychange', handleVisibilityChange);

    // Dynamic background sync every 8 seconds to stay seamlessly in sync with the Android admin app
    const interval = setInterval(() => {
      loadData(true);
    }, 8000);

    return () => {
      window.removeEventListener('focus', handleFocus);
      document.removeEventListener('visibilitychange', handleVisibilityChange);
      clearInterval(interval);
    };
  }, []);

  const loadData = async (silent = false) => {
    try {
      const [profileRes, projectsRes, skillsRes] = await Promise.all([
        profileAPI.get(),
        projectsAPI.getAll(),
        skillsAPI.getAll()
      ]);
      setProfile(profileRes.data);
      setProjects(projectsRes.data);
      setSkills(skillsRes.data);
    } catch (err) {
      console.error('Error loading data:', err);
    }
    if (!silent) setLoading(false);
  };

  const handleContactSubmit = async (e) => {
    e.preventDefault();
    setSending(true);
    setFormStatus(null);
    try {
      await contactsAPI.send(contactForm);
      setFormStatus({ type: 'success', message: 'Message sent successfully! I\'ll get back to you soon.' });
      setContactForm({ name: '', email: '', subject: '', message: '' });
    } catch (err) {
      setFormStatus({ type: 'error', message: 'Failed to send message. Please try again.' });
    }
    setSending(false);
  };

  // Group skills by category
  const skillsByCategory = skills.reduce((acc, skill) => {
    const cat = skill.category || 'Other';
    if (!acc[cat]) acc[cat] = [];
    acc[cat].push(skill);
    return acc;
  }, {});

  if (loading) {
    return (
      <div className="page-loader">
        <div className="spinner"></div>
      </div>
    );
  }

  const avatarUrl = profile?.avatar_url || '/mannequin.png';

  const containerVariants = {
    hidden: { opacity: 0 },
    visible: {
      opacity: 1,
      transition: { staggerChildren: 0.15 }
    }
  };

  const itemVariants = {
    hidden: { opacity: 0, y: 20 },
    visible: { opacity: 1, y: 0, transition: { duration: 0.6 } }
  };

  return (
    <>
      <Navbar />

      {/* ===== HERO ===== */}
      <section id="home" className="hero">
        <ParticlesBg />
        <div className="container">
          <div className="hero-content">
            <motion.div 
              className="hero-text"
              variants={containerVariants}
              initial="hidden"
              animate="visible"
            >
              <motion.p variants={itemVariants} className="hero-greeting">Hello, I'm</motion.p>
              <motion.h1 variants={itemVariants} className="hero-name">{profile?.name || 'Narayan Phukan'}</motion.h1>
              <motion.p variants={itemVariants} className="hero-title">{profile?.title || '5th Semester B.Sc Computer Science'}</motion.p>
              <motion.p variants={itemVariants} className="hero-desc">
                {profile?.bio || 'Passionate computer science student and remote developer building modern web applications.'}
              </motion.p>
              <motion.div variants={itemVariants} className="hero-buttons">
                <a href="#contact" className="btn btn-primary btn-lg" onClick={(e) => { e.preventDefault(); document.querySelector('#contact')?.scrollIntoView({ behavior: 'smooth' }); }}>
                  <FiSend /> Get in Touch
                </a>
                <a href="#projects" className="btn btn-outline-light btn-lg" onClick={(e) => { e.preventDefault(); document.querySelector('#projects')?.scrollIntoView({ behavior: 'smooth' }); }}>
                  <FiCode /> View Projects
                </a>
              </motion.div>
              <motion.div variants={itemVariants} className="hero-social">
                {profile?.github_url && (
                  <a href={profile.github_url} target="_blank" rel="noopener noreferrer" aria-label="GitHub"><FiGithub /></a>
                )}
                {profile?.linkedin_url && (
                  <a href={profile.linkedin_url} target="_blank" rel="noopener noreferrer" aria-label="LinkedIn"><FiLinkedin /></a>
                )}
                {profile?.instagram_url && (
                  <a href={profile.instagram_url} target="_blank" rel="noopener noreferrer" aria-label="Instagram"><FiInstagram /></a>
                )}
                <a href={`mailto:${profile?.email || 'narayanphukan30@gmail.com'}`} aria-label="Email"><FiMail /></a>
              </motion.div>
            </motion.div>
            <motion.div 
              initial={{ opacity: 0, scale: 0.8, rotate: -5 }}
              animate={{ opacity: 1, scale: 1, rotate: 0 }}
              transition={{ duration: 0.8, delay: 0.3 }}
              className="hero-image-wrapper"
            >
              <img src={avatarUrl} alt={profile?.name || 'Narayan Phukan'} className="hero-image" />
            </motion.div>
          </div>
        </div>
      </section>

      {/* ===== ABOUT ===== */}
      <section id="about" className="section">
        <div className="container">
          <motion.div 
            initial={{ opacity: 0, y: 30 }}
            whileInView={{ opacity: 1, y: 0 }}
            viewport={{ once: true, margin: "-100px" }}
            transition={{ duration: 0.6 }}
            className="section-title"
          >
            <h2 className="gradient-text">About Me</h2>
            <p>Get to know more about my journey and passion</p>
          </motion.div>
          <div className="about-grid">
            <motion.div 
              initial={{ opacity: 0, x: -50 }}
              whileInView={{ opacity: 1, x: 0 }}
              viewport={{ once: true, margin: "-100px" }}
              transition={{ duration: 0.6, delay: 0.2 }}
              className="about-image-container"
            >
              <img src={avatarUrl} alt={profile?.name || 'Narayan Phukan'} className="about-image" />
            </motion.div>
            <motion.div 
              initial={{ opacity: 0, x: 50 }}
              whileInView={{ opacity: 1, x: 0 }}
              viewport={{ once: true, margin: "-100px" }}
              transition={{ duration: 0.6, delay: 0.4 }}
              className="about-text"
            >
              <h3>A passionate developer who loves creating things</h3>
              <p>{profile?.about_text || 'I am a dedicated Computer Science student currently in my 5th semester of B.Sc. I work remotely, building modern web applications with cutting-edge technologies.'}</p>
              <p>I believe in writing clean, efficient code and creating user experiences that are both beautiful and functional. My goal is to leverage technology to solve real-world problems.</p>
              <div className="about-info">
                <div className="about-info-item">
                  <div className="icon"><FiMail /></div>
                  <div>
                    <div className="label">Email</div>
                    <div className="value">{profile?.email || 'narayanphukan30@gmail.com'}</div>
                  </div>
                </div>
                <div className="about-info-item">
                  <div className="icon"><FiPhone /></div>
                  <div>
                    <div className="label">Phone</div>
                    <div className="value">{profile?.phone || '+91-6001015041'}</div>
                  </div>
                </div>
                <div className="about-info-item">
                  <div className="icon"><FiMapPin /></div>
                  <div>
                    <div className="label">Location</div>
                    <div className="value">{profile?.location || 'India'}</div>
                  </div>
                </div>
                <div className="about-info-item">
                  <div className="icon"><FiBriefcase /></div>
                  <div>
                    <div className="label">Work</div>
                    <div className="value">Remote Developer</div>
                  </div>
                </div>
              </div>
            </motion.div>
          </div>
        </div>
      </section>

      {/* ===== PROJECTS ===== */}
      <section id="projects" className="section section-alt">
        <div className="container">
          <motion.div 
            initial={{ opacity: 0, y: 30 }}
            whileInView={{ opacity: 1, y: 0 }}
            viewport={{ once: true, margin: "-100px" }}
            transition={{ duration: 0.6 }}
            className="section-title"
          >
            <h2 className="gradient-text">My Projects</h2>
            <p>A showcase of my recent work and contributions</p>
          </motion.div>
          {projects.length === 0 ? (
            <div className="no-data-message">
              <div className="icon"><FiCode /></div>
              <p>Projects coming soon! Check back later.</p>
            </div>
          ) : (
            <motion.div 
              variants={containerVariants}
              initial="hidden"
              whileInView="visible"
              viewport={{ once: true, margin: "-100px" }}
              className="projects-grid"
            >
              {projects.map((project) => (
                <motion.div variants={itemVariants} key={project.id} className="project-card">
                  {project.image_url && (
                    <div style={{ overflow: 'hidden' }}>
                      <img src={project.image_url} alt={project.title} className="project-card-image" />
                    </div>
                  )}
                  <div className="project-card-body">
                    <h3>{project.title}</h3>
                    <p>{project.description}</p>
                    {project.tech_stack?.length > 0 && (
                      <div className="project-tech">
                        {project.tech_stack.map((tech, i) => (
                          <span key={i}>{tech}</span>
                        ))}
                      </div>
                    )}
                    <div className="project-links">
                      {project.live_url && (
                        <a href={project.live_url} target="_blank" rel="noopener noreferrer">
                          <FiExternalLink /> Live Demo
                        </a>
                      )}
                      {project.github_url && (
                        <a href={project.github_url} target="_blank" rel="noopener noreferrer">
                          <FiGithub /> Source Code
                        </a>
                      )}
                    </div>
                  </div>
                </motion.div>
              ))}
            </motion.div>
          )}
        </div>
      </section>

      {/* ===== SKILLS ===== */}
      <section id="skills" className="section">
        <div className="container">
          <motion.div 
            initial={{ opacity: 0, y: 30 }}
            whileInView={{ opacity: 1, y: 0 }}
            viewport={{ once: true, margin: "-100px" }}
            transition={{ duration: 0.6 }}
            className="section-title"
          >
            <h2 className="gradient-text">Skills & Expertise</h2>
            <p>Technologies and tools I work with</p>
          </motion.div>
          {skills.length === 0 ? (
            <div className="no-data-message">
              <div className="icon"><FiCode /></div>
              <p>Skills will be listed here soon!</p>
            </div>
          ) : (
            <motion.div 
              variants={containerVariants}
              initial="hidden"
              whileInView="visible"
              viewport={{ once: true, margin: "-100px" }}
              className="skills-categories"
            >
              {Object.entries(skillsByCategory).map(([category, categorySkills]) => (
                <motion.div variants={itemVariants} key={category} className="skill-category">
                  <h3><FiCode /> {category}</h3>
                  {categorySkills.map((skill) => (
                    <div key={skill.id} className="skill-item">
                      <div className="skill-header">
                        <span>{skill.name}</span>
                        <span>{skill.proficiency}%</span>
                      </div>
                      <div className="skill-bar">
                        <div className="skill-bar-fill" style={{ width: `${skill.proficiency}%` }}></div>
                      </div>
                    </div>
                  ))}
                  </motion.div>
              ))}
            </motion.div>
          )}
        </div>
      </section>

      {/* ===== HIRE ME ===== */}
      <section id="hire" className="section hire-section">
        <div className="container">
          <motion.div 
            initial={{ opacity: 0, y: 40 }}
            whileInView={{ opacity: 1, y: 0 }}
            viewport={{ once: true, margin: "-100px" }}
            transition={{ duration: 0.7 }}
            className="hire-content"
          >
            <div className="hire-status">
              <span className="dot"></span>
              {profile?.available_for_hire ? 'Available for Work' : 'Currently Busy'}
            </div>
            <h2>Let's Work Together</h2>
            <p>
              I'm a remote developer passionate about building exceptional digital experiences.
              Whether you need a full-stack web application, a sleek frontend, or help with your existing project — I'd love to hear from you.
            </p>
            <p>
              I bring dedication, clean code practices, and a keen eye for design to every project.
            </p>
            <div className="hire-buttons">
              <a href={`mailto:${profile?.email || 'narayanphukan30@gmail.com'}`} className="btn btn-primary btn-lg">
                <FiMail /> Email Me
              </a>
              <a href="#contact" className="btn btn-outline-light btn-lg" onClick={(e) => { e.preventDefault(); document.querySelector('#contact')?.scrollIntoView({ behavior: 'smooth' }); }}>
                <FiSend /> Send a Message
              </a>
              {profile?.resume_url && (
                <a href={profile.resume_url} target="_blank" rel="noopener noreferrer" className="btn btn-outline-light btn-lg">
                  <FiDownload /> Download CV
                </a>
              )}
            </div>
          </motion.div>
        </div>
      </section>

      {/* ===== CONTACT ===== */}
      <section id="contact" className="section">
        <div className="container">
          <motion.div 
            initial={{ opacity: 0, y: 30 }}
            whileInView={{ opacity: 1, y: 0 }}
            viewport={{ once: true, margin: "-100px" }}
            transition={{ duration: 0.6 }}
            className="section-title"
          >
            <h2 className="gradient-text">Get In Touch</h2>
            <p>Have a question or want to work together? Drop me a message!</p>
          </motion.div>
          <div className="contact-grid">
            <motion.div 
              initial={{ opacity: 0, x: -40 }}
              whileInView={{ opacity: 1, x: 0 }}
              viewport={{ once: true, margin: "-100px" }}
              transition={{ duration: 0.6, delay: 0.2 }}
              className="contact-info"
            >
              <h3>Contact Information</h3>
              <p>Feel free to reach out. I'm always open to discussing new projects, ideas, or opportunities.</p>
              <div className="contact-item">
                <div className="icon"><FiMail /></div>
                <div className="details">
                  <h4>Email</h4>
                  <p>{profile?.email || 'narayanphukan30@gmail.com'}</p>
                </div>
              </div>
              <div className="contact-item">
                <div className="icon"><FiPhone /></div>
                <div className="details">
                  <h4>Phone</h4>
                  <p>{profile?.phone || '+91-6001015041'}</p>
                </div>
              </div>
              <div className="contact-item">
                <div className="icon"><FiMapPin /></div>
                <div className="details">
                  <h4>Location</h4>
                  <p>{profile?.location || 'India'}</p>
                </div>
              </div>
            </motion.div>
            <motion.form 
              initial={{ opacity: 0, x: 40 }}
              whileInView={{ opacity: 1, x: 0 }}
              viewport={{ once: true, margin: "-100px" }}
              transition={{ duration: 0.6, delay: 0.4 }}
              className="contact-form" 
              onSubmit={handleContactSubmit}
            >
              <div className="form-row">
                <div className="form-group">
                  <label htmlFor="contact-name">Your Name</label>
                  <input
                    id="contact-name"
                    type="text"
                    placeholder="John Doe"
                    value={contactForm.name}
                    onChange={(e) => setContactForm({ ...contactForm, name: e.target.value })}
                    required
                  />
                </div>
                <div className="form-group">
                  <label htmlFor="contact-email">Your Email</label>
                  <input
                    id="contact-email"
                    type="email"
                    placeholder="john@example.com"
                    value={contactForm.email}
                    onChange={(e) => setContactForm({ ...contactForm, email: e.target.value })}
                    required
                  />
                </div>
              </div>
              <div className="form-group">
                <label htmlFor="contact-subject">Subject</label>
                <input
                  id="contact-subject"
                  type="text"
                  placeholder="Project Inquiry"
                  value={contactForm.subject}
                  onChange={(e) => setContactForm({ ...contactForm, subject: e.target.value })}
                />
              </div>
              <div className="form-group">
                <label htmlFor="contact-message">Message</label>
                <textarea
                  id="contact-message"
                  placeholder="Tell me about your project..."
                  value={contactForm.message}
                  onChange={(e) => setContactForm({ ...contactForm, message: e.target.value })}
                  required
                />
              </div>
              {formStatus && (
                <div className={formStatus.type === 'success' ? 'form-success' : 'form-error'}>
                  {formStatus.message}
                </div>
              )}
              <button type="submit" className="btn btn-primary btn-lg" disabled={sending} style={{ width: '100%', justifyContent: 'center', marginTop: '12px' }}>
                {sending ? <><span className="spinner"></span> Sending...</> : <><FiSend /> Send Message</>}
              </button>
            </motion.form>
          </div>
        </div>
      </section>

      <Footer profile={profile} />
    </>
  );
}
