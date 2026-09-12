const { createClient } = require('@supabase/supabase-js');
const admin = require('firebase-admin');
const { getFirestore } = require('firebase-admin/firestore');
const serviceAccount = require('../firebase-service-account.json');
require('dotenv').config({ path: require('path').resolve(__dirname, '../.env') });

admin.initializeApp({
  credential: admin.cert(serviceAccount)
});

const firestore = getFirestore();
const supabase = createClient(process.env.SUPABASE_URL, process.env.SUPABASE_SERVICE_KEY);

async function migrate() {
  console.log('🚀 Starting migration from Supabase to Cloud Firestore...');

  // 1. Profile
  console.log('Migrating Profile...');
  const { data: profileData, error: profileError } = await supabase
    .from('portfolio_profile')
    .select('*');

  if (profileError) {
    console.error('Error fetching profile from Supabase:', profileError);
  } else if (profileData && profileData.length > 0) {
    const profile = profileData[0];
    await firestore.collection('profile').doc('main').set(profile);
    // Also save under its original id if needed
    if (profile.id) {
      await firestore.collection('profile').doc(profile.id).set(profile);
    }
    console.log(`✅ Migrated profile for ${profile.name}`);
  }

  // 2. Projects
  console.log('Migrating Projects...');
  const { data: projectsData, error: projectsError } = await supabase
    .from('portfolio_projects')
    .select('*')
    .order('display_order', { ascending: true });

  if (projectsError) {
    console.error('Error fetching projects from Supabase:', projectsError);
  } else if (projectsData) {
    for (const proj of projectsData) {
      const docId = proj.id || firestore.collection('projects').doc().id;
      await firestore.collection('projects').doc(docId).set({
        ...proj,
        id: docId
      });
      console.log(`  - Project: "${proj.title}"`);
    }
    console.log(`✅ Migrated ${projectsData.length} projects.`);
  }

  // 3. Skills
  console.log('Migrating Skills...');
  const { data: skillsData, error: skillsError } = await supabase
    .from('portfolio_skills')
    .select('*')
    .order('display_order', { ascending: true });

  if (skillsError) {
    console.error('Error fetching skills from Supabase:', skillsError);
  } else if (skillsData) {
    for (const skill of skillsData) {
      const docId = skill.id || firestore.collection('skills').doc().id;
      await firestore.collection('skills').doc(docId).set({
        ...skill,
        id: docId
      });
      console.log(`  - Skill: ${skill.name} (${skill.proficiency}%)`);
    }
    console.log(`✅ Migrated ${skillsData.length} skills.`);
  }

  // 4. Contacts
  console.log('Migrating Contacts...');
  const { data: contactsData, error: contactsError } = await supabase
    .from('portfolio_contacts')
    .select('*');

  if (contactsError) {
    console.error('Error fetching contacts from Supabase:', contactsError);
  } else if (contactsData && contactsData.length > 0) {
    for (const contact of contactsData) {
      const docId = contact.id || firestore.collection('contacts').doc().id;
      await firestore.collection('contacts').doc(docId).set({
        ...contact,
        id: docId
      });
    }
    console.log(`✅ Migrated ${contactsData.length} contacts.`);
  } else {
    console.log('ℹ️ No contacts to migrate.');
  }

  console.log('🎉 Migration completed successfully!');
  process.exit(0);
}

migrate().catch((err) => {
  console.error('Migration failed:', err);
  process.exit(1);
});
