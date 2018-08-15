-- 1. Add column to hold license identifier
ALTER TABLE book ADD COLUMN license text NOT NULL DEFAULT 'CC-BY-4.0';

-- 2. Update with correct licenseidentifier
UPDATE book AS b
SET license = upper(l.name)
FROM license AS l
WHERE b.license_id = l.id;

-- 3. Remove old license information
ALTER TABLE book DROP COLUMN license_id;
drop table license;
