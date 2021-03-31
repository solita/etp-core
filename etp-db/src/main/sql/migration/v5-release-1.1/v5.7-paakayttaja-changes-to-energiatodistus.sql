ALTER TABLE energiatodistus ADD COLUMN draft_visible_to_paakayttaja boolean NOT NULL DEFAULT false;
ALTER TABLE energiatodistus ADD COLUMN bypass_validation_limits boolean NOT NULL DEFAULT false;
