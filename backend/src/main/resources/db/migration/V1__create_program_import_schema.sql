CREATE
    TABLE
        data_import(
            id BIGINT NOT NULL AUTO_INCREMENT,
            source_type VARCHAR(50) NOT NULL,
            data_version VARCHAR(255) NOT NULL,
            file_name VARCHAR(255),
            file_checksum CHAR(64),
            source_locator VARCHAR(1024),
            request_signature CHAR(64),
            collected_from_ym CHAR(6),
            collected_to_ym CHAR(6),
            status VARCHAR(20) NOT NULL,
            total_count INT NOT NULL DEFAULT 0,
            success_count INT NOT NULL DEFAULT 0,
            failure_count INT NOT NULL DEFAULT 0,
            last_error_message VARCHAR(1000),
            started_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP( 6 ),
            completed_at DATETIME(6),
            created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP( 6 ),
            updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP( 6 ) ON
            UPDATE
                CURRENT_TIMESTAMP( 6 ),
                PRIMARY KEY(id),
                CONSTRAINT uk_data_import_file UNIQUE(
                    source_type,
                    file_checksum
                ),
                CONSTRAINT uk_data_import_request UNIQUE(
                    source_type,
                    request_signature
                ),
                CONSTRAINT ck_data_import_source_type CHECK(
                    source_type IN(
                        'PUBLIC_FACILITY_PROGRAM',
                        'FITNESS_MEASUREMENT_API'
                    )
                ),
                CONSTRAINT ck_data_import_status CHECK(
                    status IN(
                        'RUNNING',
                        'COMPLETED',
                        'FAILED'
                    )
                ),
                CONSTRAINT ck_data_import_source_fields CHECK(
                    (
                        source_type = 'PUBLIC_FACILITY_PROGRAM'
                        AND file_name IS NOT NULL
                        AND file_checksum IS NOT NULL
                        AND source_locator IS NULL
                        AND request_signature IS NULL
                        AND collected_from_ym IS NULL
                        AND collected_to_ym IS NULL
                    )
                    OR(
                        source_type = 'FITNESS_MEASUREMENT_API'
                        AND file_name IS NULL
                        AND file_checksum IS NULL
                        AND source_locator IS NOT NULL
                        AND request_signature IS NOT NULL
                        AND collected_from_ym IS NOT NULL
                        AND collected_to_ym IS NOT NULL
                    )
                ),
                CONSTRAINT ck_data_import_counts CHECK(
                    total_count >= 0
                    AND success_count >= 0
                    AND failure_count >= 0
                    AND success_count + failure_count <= total_count
                ),
                CONSTRAINT ck_data_import_completion CHECK(
                    (
                        status = 'RUNNING'
                        AND completed_at IS NULL
                    )
                    OR(
                        status IN(
                            'COMPLETED',
                            'FAILED'
                        )
                        AND completed_at IS NOT NULL
                    )
                ),
                INDEX idx_data_import_latest_completed(
                    source_type,
                    status,
                    completed_at
                )
        );

CREATE
    TABLE
        facility(
            id BIGINT NOT NULL AUTO_INCREMENT,
            source_key CHAR(64) NOT NULL,
            name VARCHAR(255) NOT NULL,
            sido_code VARCHAR(20) NOT NULL,
            sido_name VARCHAR(100) NOT NULL,
            sigungu_code VARCHAR(20) NOT NULL,
            sigungu_name VARCHAR(100) NOT NULL,
            emd_name VARCHAR(100),
            address VARCHAR(500) NOT NULL,
            latitude DECIMAL(
                10,
                7
            ),
            longitude DECIMAL(
                10,
                7
            ),
            phone_number VARCHAR(30),
            homepage_url VARCHAR(2048),
            created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP( 6 ),
            updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP( 6 ) ON
            UPDATE
                CURRENT_TIMESTAMP( 6 ),
                PRIMARY KEY(id),
                CONSTRAINT uk_facility_source_key UNIQUE(source_key),
                CONSTRAINT ck_facility_latitude CHECK(
                    latitude BETWEEN - 90 AND 90
                ),
                CONSTRAINT ck_facility_longitude CHECK(
                    longitude BETWEEN - 180 AND 180
                ),
                INDEX idx_facility_region(
                    sido_code,
                    sigungu_code
                )
        );

CREATE
    TABLE
        program(
            id BIGINT NOT NULL AUTO_INCREMENT,
            facility_id BIGINT NOT NULL,
            import_id BIGINT NOT NULL,
            source_key CHAR(64) NOT NULL,
            type_name VARCHAR(255),
            name VARCHAR(255) NOT NULL,
            target_name VARCHAR(255) NOT NULL,
            starts_on DATE NOT NULL,
            ends_on DATE NOT NULL,
            weekday_text VARCHAR(255) NOT NULL,
            weekday_mask TINYINT UNSIGNED,
            time_text VARCHAR(255) NOT NULL,
            recruitment_capacity INT,
            price DECIMAL(
                12,
                2
            ),
            price_type_name VARCHAR(100),
            program_category VARCHAR(50) NOT NULL,
            adult_eligibility VARCHAR(30) NOT NULL,
            normalization_status VARCHAR(30) NOT NULL,
            created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP( 6 ),
            updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP( 6 ) ON
            UPDATE
                CURRENT_TIMESTAMP( 6 ),
                PRIMARY KEY(id),
                CONSTRAINT uk_program_source_key UNIQUE(source_key),
                CONSTRAINT fk_program_facility FOREIGN KEY(facility_id) REFERENCES facility(id),
                CONSTRAINT fk_program_import FOREIGN KEY(import_id) REFERENCES data_import(id),
                CONSTRAINT ck_program_dates CHECK(
                    ends_on >= starts_on
                ),
                CONSTRAINT ck_program_weekday_mask CHECK(
                    weekday_mask BETWEEN 1 AND 127
                ),
                CONSTRAINT ck_program_capacity CHECK(
                    recruitment_capacity IS NULL
                    OR recruitment_capacity >= 0
                ),
                CONSTRAINT ck_program_price CHECK(
                    price IS NULL
                    OR price >= 0
                ),
                CONSTRAINT ck_program_category CHECK(
                    program_category IN(
                        'SWIMMING_AQUA',
                        'FITNESS_STRENGTH',
                        'YOGA_PILATES',
                        'CARDIO',
                        'DANCE_AEROBIC',
                        'RACKET_SPORTS',
                        'BALL_SPORTS',
                        'MARTIAL_ARTS',
                        'CLIMBING',
                        'GOLF',
                        'OTHER'
                    )
                ),
                CONSTRAINT ck_program_adult_eligibility CHECK(
                    adult_eligibility IN(
                        'ADULT_EXPLICIT',
                        'ADULT_POSSIBLE',
                        'UNKNOWN',
                        'CHILD_ONLY'
                    )
                ),
                INDEX idx_program_facility_ends_on(
                    facility_id,
                    ends_on
                )
        );
