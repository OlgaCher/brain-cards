-- ============================================================
-- BrainCards — seed content (UA / EN)
-- 5 zones, NO-MATERIALS games only (nothing to print, buy or draw).
-- Matches the Hibernate-generated schema: zone / zone_translation / game / game_translation.
-- Run AFTER Hibernate has created the tables (spring.jpa.hibernate.ddl-auto=update),
-- i.e. after the app has booted at least once.
--
-- Locale code is 'UA' (not the ISO 'uk') to match this project's convention - see
-- LocaleConfig.java / GameService.java for why. Zone.code is UNIQUE, so re-running this
-- script against an already-seeded database will fail loudly on the first duplicate zone
-- rather than silently duplicating content. To re-seed from scratch:
--   TRUNCATE game_translation, game, zone_translation, zone RESTART IDENTITY CASCADE;
--
-- Each game insert uses `WITH new_game AS (INSERT ... RETURNING id)` rather than
-- currval('game_id_seq') - this doesn't depend on knowing/guessing what Hibernate/Postgres
-- named the identity column's backing sequence, so it works regardless of generator strategy.
-- ============================================================

-- ------------------------------------------------------------
-- ZONES
-- ------------------------------------------------------------
INSERT INTO zone (code) VALUES
  ('regulation'),
  ('interhemispheric'),
  ('spatial'),
  ('gross_motor'),
  ('fine_motor');

INSERT INTO zone_translation (zone_id, locale, name)
SELECT id, 'UA', 'Регуляторний компонент'      FROM zone WHERE code='regulation'
UNION ALL SELECT id, 'en', 'Self-regulation'    FROM zone WHERE code='regulation'
UNION ALL SELECT id, 'UA', 'Міжпівкульна взаємодія' FROM zone WHERE code='interhemispheric'
UNION ALL SELECT id, 'en', 'Interhemispheric interaction' FROM zone WHERE code='interhemispheric'
UNION ALL SELECT id, 'UA', 'Просторова орієнтація' FROM zone WHERE code='spatial'
UNION ALL SELECT id, 'en', 'Spatial orientation'  FROM zone WHERE code='spatial'
UNION ALL SELECT id, 'UA', 'Крупна моторика'      FROM zone WHERE code='gross_motor'
UNION ALL SELECT id, 'en', 'Gross motor skills'   FROM zone WHERE code='gross_motor'
UNION ALL SELECT id, 'UA', 'Дрібна моторика'      FROM zone WHERE code='fine_motor'
UNION ALL SELECT id, 'en', 'Fine motor skills'    FROM zone WHERE code='fine_motor';

-- ============================================================
-- ZONE 1 — РЕГУЛЯТОРНИЙ КОМПОНЕНТ / SELF-REGULATION
-- ============================================================

-- 1.1 Акула / Shark
WITH new_game AS (
  INSERT INTO game (zone_id, min_age_months, max_age_months, active, cooldown_days)
  SELECT id, 36, 72, true, NULL FROM zone WHERE code='regulation'
  RETURNING id
)
INSERT INTO game_translation (game_id, locale, title, instructions)
SELECT id, 'UA', 'Акула',
  'Дорослий — «акула», дитина вільно рухається кімнатою. Поки акула «спить», можна ходити й бігати. За сигналом «Акула!» треба завмерти на місці — акула бачить лише те, що рухається. Хто ворухнувся — того «спіймали». Починайте з коротких пауз (3–5 секунд), поступово подовжуйте. Вправа тренує гальмування імпульсу та утримання команди.' FROM new_game
UNION ALL
SELECT id, 'en', 'Shark',
  'The adult is the "shark"; the child moves freely around the room. While the shark sleeps, walking and running are allowed. On the signal "Shark!" the child must freeze — the shark only sees what moves. Whoever moves is caught. Start with short pauses (3–5 seconds) and gradually make them longer. Trains impulse inhibition and holding a rule in mind.' FROM new_game;

-- 1.2 Підлога-ніс-стеля / Floor–nose–ceiling
WITH new_game AS (
  INSERT INTO game (zone_id, min_age_months, max_age_months, active, cooldown_days)
  SELECT id, 36, 72, true, NULL FROM zone WHERE code='regulation'
  RETURNING id
)
INSERT INTO game_translation (game_id, locale, title, instructions)
SELECT id, 'UA', 'Підлога — ніс — стеля',
  'Дорослий називає «підлога», «ніс» або «стеля» і одночасно показує рукою. Дитина має показувати те, що ЗВУЧИТЬ, а не те, що бачить. Спочатку слово і жест збігаються, далі дорослий починає «плутати»: каже «ніс», а показує на стелю. Дитина має втриматись і показати ніс. Тренує довільну увагу та подолання наслідування.' FROM new_game
UNION ALL
SELECT id, 'en', 'Floor — nose — ceiling',
  'The adult says "floor", "nose" or "ceiling" while pointing. The child must follow what they HEAR, not what they see. At first word and gesture match; then the adult starts mismatching them — saying "nose" while pointing at the ceiling. The child must resist and point to their nose. Trains voluntary attention and overriding imitation.' FROM new_game;

-- 1.3 Саймон каже / Simon says
WITH new_game AS (
  INSERT INTO game (zone_id, min_age_months, max_age_months, active, cooldown_days)
  SELECT id, 36, 72, true, NULL FROM zone WHERE code='regulation'
  RETURNING id
)
INSERT INTO game_translation (game_id, locale, title, instructions)
SELECT id, 'UA', 'Саймон каже',
  'Дорослий дає команди («підстрибни», «підніми руки»). Виконувати треба лише ті, що починаються словами «Саймон каже». Якщо команда прозвучала без цих слів — рухатись не можна. Починайте повільно, з явними паузами; далі пришвидшуйте темп. Тренує утримання правила та гальмування автоматичної реакції.' FROM new_game
UNION ALL
SELECT id, 'en', 'Simon says',
  'The adult gives commands ("jump", "raise your hands"). The child performs only those that begin with "Simon says". If the words are missing, they must not move. Start slowly with clear pauses, then speed up. Trains rule-holding and inhibition of an automatic response.' FROM new_game;

-- 1.4 Заборонений рух / Forbidden movement
WITH new_game AS (
  INSERT INTO game (zone_id, min_age_months, max_age_months, active, cooldown_days)
  SELECT id, 36, 72, true, NULL FROM zone WHERE code='regulation'
  RETURNING id
)
INSERT INTO game_translation (game_id, locale, title, instructions)
SELECT id, 'UA', 'Заборонений рух',
  'Домовтесь про один рух, який повторювати НЕ можна — наприклад, руки на пояс. Далі дорослий показує різні рухи, а дитина повторює за ним усі, крім забороненого. Коли з''являється заборонений — треба просто стояти. Згодом заборонених рухів може стати два. Тренує вибіркове гальмування.' FROM new_game
UNION ALL
SELECT id, 'en', 'Forbidden movement',
  'Agree on one movement that must NOT be copied — for example, hands on hips. The adult then performs various movements and the child copies them all except the forbidden one. When it appears, the child simply stands still. Later you can add a second forbidden movement. Trains selective inhibition.' FROM new_game;

-- 1.5 Зміни рух / Switch the movement
WITH new_game AS (
  INSERT INTO game (zone_id, min_age_months, max_age_months, active, cooldown_days)
  SELECT id, 48, 72, true, NULL FROM zone WHERE code='regulation'
  RETURNING id
)
INSERT INTO game_translation (game_id, locale, title, instructions)
SELECT id, 'UA', 'Зміни рух',
  'Оберіть три рухи: стук ногою в підлогу, плескіт у долоні, присідання. Правило: на плескіт дорослого дитина стукає ногою; на стук ногою — плескає в долоні; присідає лише тоді, коли присідає дорослий. Тобто два рухи «міняються місцями», а третій повторюється прямо. Починайте дуже повільно. Тренує перемикання та утримання складної інструкції.' FROM new_game
UNION ALL
SELECT id, 'en', 'Switch the movement',
  'Choose three movements: stamping a foot, clapping, squatting. The rule: when the adult claps, the child stamps; when the adult stamps, the child claps; the child squats only when the adult squats. So two movements are swapped and the third is copied directly. Start very slowly. Trains switching and holding a complex instruction.' FROM new_game;

-- 1.6 Дзеркало / Mirror
WITH new_game AS (
  INSERT INTO game (zone_id, min_age_months, max_age_months, active, cooldown_days)
  SELECT id, 36, 72, true, NULL FROM zone WHERE code='regulation'
  RETURNING id
)
INSERT INTO game_translation (game_id, locale, title, instructions)
SELECT id, 'UA', 'Дзеркало',
  'Станьте одне навпроти одного. Дорослий виконує рух, дитина точно його повторює — з увагою до просторових характеристик: якщо дорослий підняв ПРАВУ руку, дитина теж піднімає ПРАВУ (а не ту, що навпроти). Рухи спочатку прості й повільні, далі складніші. Тренує довільну регуляцію та просторовий контроль рухів.' FROM new_game
UNION ALL
SELECT id, 'en', 'Mirror',
  'Stand facing each other. The adult makes a movement and the child copies it exactly, paying attention to its spatial features: if the adult raises their RIGHT hand, the child raises their RIGHT hand too (not the one opposite). Start with simple, slow movements and build up. Trains voluntary regulation and spatial control of movement.' FROM new_game;

-- 1.7 Черепашачі перегони / Turtle race
WITH new_game AS (
  INSERT INTO game (zone_id, min_age_months, max_age_months, active, cooldown_days)
  SELECT id, 36, 72, true, NULL FROM zone WHERE code='regulation'
  RETURNING id
)
INSERT INTO game_translation (game_id, locale, title, instructions)
SELECT id, 'UA', 'Черепашачі перегони',
  'Оголосіть, що ви обоє — черепахи. Позначте старт і фініш. За сигналом починайте рух, але перемагає той, хто прийде до фінішу ОСТАННІМ. Зупинятись не можна — треба рухатись увесь час, тільки дуже повільно. Вправа складна саме тим, що вимагає гальмувати природне бажання поспішати.' FROM new_game
UNION ALL
SELECT id, 'en', 'Turtle race',
  'Announce that you are both turtles. Mark a start and a finish. On the signal you both set off — but the winner is whoever reaches the finish LAST. Stopping is not allowed: you must keep moving, only very slowly. The difficulty is exactly the point — it requires inhibiting the natural urge to hurry.' FROM new_game;

-- 1.8 Переказ історії / Retelling a story
WITH new_game AS (
  INSERT INTO game (zone_id, min_age_months, max_age_months, active, cooldown_days)
  SELECT id, 48, 72, true, NULL FROM zone WHERE code='regulation'
  RETURNING id
)
INSERT INTO game_translation (game_id, locale, title, instructions)
SELECT id, 'UA', 'Переказ історії',
  'Прочитайте або розкажіть коротку історію, а потім попросіть дитину переказати її своїми словами. Допомагайте питаннями про послідовність: «що було спочатку?», «а що сталося потім?», «чим усе закінчилось?». Важлива не дослівність, а утримання плану розповіді. Тренує програмування та контроль власного мовлення.' FROM new_game
UNION ALL
SELECT id, 'en', 'Retelling a story',
  'Read or tell a short story, then ask the child to retell it in their own words. Support them with questions about sequence: "What happened first?", "And then?", "How did it end?". Word-for-word accuracy does not matter — holding the structure of the story does. Trains planning and monitoring of one''s own speech.' FROM new_game;

-- ============================================================
-- ZONE 2 — МІЖПІВКУЛЬНА ВЗАЄМОДІЯ / INTERHEMISPHERIC INTERACTION
-- ============================================================

-- 2.1 Колечки / Rings
WITH new_game AS (
  INSERT INTO game (zone_id, min_age_months, max_age_months, active, cooldown_days)
  SELECT id, 48, 72, true, NULL FROM zone WHERE code='interhemispheric'
  RETURNING id
)
INSERT INTO game_translation (game_id, locale, title, instructions)
SELECT id, 'UA', 'Колечки',
  'Почергово і швидко з''єднуйте пальці в кільце з великим: вказівний — середній — безіменний — мізинець. Спочатку однією рукою, потім другою, далі — обома одночасно. Найскладніший варіант: одна рука йде від вказівного до мізинця, а друга — у зворотному напрямку. Тренує узгоджену роботу обох рук.' FROM new_game
UNION ALL
SELECT id, 'en', 'Rings',
  'One after another, quickly touch each finger to the thumb to form a ring: index — middle — ring — little finger. First with one hand, then the other, then both together. The hardest version: one hand goes from index to little finger while the other goes in the opposite direction. Trains coordinated work of both hands.' FROM new_game;

-- 2.2 Ок-один / OK–One
WITH new_game AS (
  INSERT INTO game (zone_id, min_age_months, max_age_months, active, cooldown_days)
  SELECT id, 48, 72, true, NULL FROM zone WHERE code='interhemispheric'
  RETURNING id
)
INSERT INTO game_translation (game_id, locale, title, instructions)
SELECT id, 'UA', 'Ок — один',
  'Одна рука показує жест «ОК» (великий і вказівний у кільце), друга — «один» (піднятий вказівний палець). За сигналом руки міняються ролями: та, що показувала «ОК», показує «один», і навпаки. Поступово пришвидшуйте зміну. Тренує різнойменні рухи рук і швидке перемикання.' FROM new_game
UNION ALL
SELECT id, 'en', 'OK — One',
  'One hand shows "OK" (thumb and index finger in a ring), the other shows "one" (index finger up). On the signal the hands swap roles: the one showing "OK" now shows "one", and vice versa. Gradually speed up the swaps. Trains different simultaneous movements of the two hands and fast switching.' FROM new_game;

-- 2.3 Фоторамка / Photo frame
WITH new_game AS (
  INSERT INTO game (zone_id, min_age_months, max_age_months, active, cooldown_days)
  SELECT id, 48, 72, true, NULL FROM zone WHERE code='interhemispheric'
  RETURNING id
)
INSERT INTO game_translation (game_id, locale, title, instructions)
SELECT id, 'UA', 'Фоторамка',
  'Складіть з пальців обох рук «рамку»: великі пальці горизонтально, вказівні — вертикально, наче кадруєте фото. Потім міняйте руки місцями — ліва зверху, права знизу і навпаки. Далі спробуйте «сфотографувати» різні предмети в кімнаті, щоразу міняючи положення рук. Тренує узгодження обох рук у просторі.' FROM new_game
UNION ALL
SELECT id, 'en', 'Photo frame',
  'Make a "frame" with both hands: thumbs horizontal, index fingers vertical, as if framing a photo. Then swap the hands — left on top, right below, and vice versa. Next, "photograph" different objects in the room, changing the hand position each time. Trains coordination of both hands in space.' FROM new_game;

-- 2.4 Вухо-ніс / Ear–nose
WITH new_game AS (
  INSERT INTO game (zone_id, min_age_months, max_age_months, active, cooldown_days)
  SELECT id, 48, 72, true, NULL FROM zone WHERE code='interhemispheric'
  RETURNING id
)
INSERT INTO game_translation (game_id, locale, title, instructions)
SELECT id, 'UA', 'Вухо — ніс',
  'Лівою рукою візьміться за кінчик носа, а правою — за протилежне (ліве) вухо. Плесніть у долоні — і поміняйте руки місцями: тепер права на носі, ліва на правому вусі. Повторюйте, поступово пришвидшуючись. Класична вправа на перехрещені рухи через середню лінію тіла.' FROM new_game
UNION ALL
SELECT id, 'en', 'Ear — nose',
  'With your left hand take hold of the tip of your nose, and with your right hand take the opposite (left) ear. Clap your hands — and swap: now the right hand is on the nose and the left on the right ear. Repeat, gradually speeding up. A classic exercise for crossing the body''s midline.' FROM new_game;

-- 2.5 Сигнали / Signals
WITH new_game AS (
  INSERT INTO game (zone_id, min_age_months, max_age_months, active, cooldown_days)
  SELECT id, 48, 72, true, NULL FROM zone WHERE code='interhemispheric'
  RETURNING id
)
INSERT INTO game_translation (game_id, locale, title, instructions)
SELECT id, 'UA', 'Сигнали',
  'Домовтесь про сигнали: один плескіт — підняти праву руку, два плескіт — підняти ліву, тупіт ногою — підняти обидві. Дорослий подає сигнали, дитина відповідає рухом. Спочатку повільно, далі швидше й у випадковому порядку. Тренує зв''язок «слух — рух» і роботу обох половин тіла.' FROM new_game
UNION ALL
SELECT id, 'en', 'Signals',
  'Agree on the signals: one clap — raise the right hand, two claps — raise the left, a stamp — raise both. The adult gives signals and the child answers with the movement. Start slowly, then go faster and in random order. Trains the link between hearing and movement, and both sides of the body.' FROM new_game;

-- 2.6 Капітан / Captain
WITH new_game AS (
  INSERT INTO game (zone_id, min_age_months, max_age_months, active, cooldown_days)
  SELECT id, 48, 72, true, NULL FROM zone WHERE code='interhemispheric'
  RETURNING id
)
INSERT INTO game_translation (game_id, locale, title, instructions)
SELECT id, 'UA', 'Капітан',
  'Одна рука біля чола «козирком», як капітан, що вдивляється вдалину. Друга рука одночасно показує «клас» — стиснутий кулак із піднятим великим пальцем. За сигналом руки міняються ролями. Пришвидшуйтесь поступово. Тренує одночасні різні рухи двох рук.' FROM new_game
UNION ALL
SELECT id, 'en', 'Captain',
  'One hand goes to the forehead like a captain shading their eyes to look into the distance. At the same time the other hand shows a "thumbs up" — a closed fist with the thumb raised. On the signal the hands swap roles. Speed up gradually. Trains performing two different hand movements at once.' FROM new_game;

-- ============================================================
-- ZONE 3 — ПРОСТОРОВА ОРІЄНТАЦІЯ / SPATIAL ORIENTATION
-- ============================================================

-- 3.1 Муха / The fly  (без картки — усно, «в уяві»)
WITH new_game AS (
  INSERT INTO game (zone_id, min_age_months, max_age_months, active, cooldown_days)
  SELECT id, 60, 72, true, NULL FROM zone WHERE code='spatial'
  RETURNING id
)
INSERT INTO game_translation (game_id, locale, title, instructions)
SELECT id, 'UA', 'Муха',
  'Усний варіант, без картки. Уявіть кімнату як поле, а в її центрі — муху. Дорослий диктує рухи: «муха полетіла вгору», «вліво», «вниз». Дитина стежить подумки й показує рукою, де муха тепер. Якщо муха «вилітає» за межі кімнати — дитина плескає в долоні. Починайте з 2–3 команд, далі більше. Тренує утримання просторового образу в уяві.' FROM new_game
UNION ALL
SELECT id, 'en', 'The fly',
  'A spoken version, no cards needed. Imagine the room as a field with a fly in the middle. The adult calls out moves: "the fly flew up", "left", "down". The child tracks it mentally and points to where the fly is now. If the fly "leaves" the room, the child claps. Start with 2–3 moves and build up. Trains holding a spatial image in the mind.' FROM new_game;

-- 3.2 Право-ліво на собі / Right and left on yourself
WITH new_game AS (
  INSERT INTO game (zone_id, min_age_months, max_age_months, active, cooldown_days)
  SELECT id, 36, 72, true, NULL FROM zone WHERE code='spatial'
  RETURNING id
)
INSERT INTO game_translation (game_id, locale, title, instructions)
SELECT id, 'UA', 'Право — ліво на собі',
  'Давайте дитині команди на власне тіло: «доторкнись лівою рукою до правого коліна», «правою рукою до лівого вуха», «лівою рукою до лівого плеча». Спочатку прості, без перехрещення, далі — перехрещені. Виконуйте повільно, дозволяючи подумати. Основа для всіх подальших просторових уявлень.' FROM new_game
UNION ALL
SELECT id, 'en', 'Right and left on yourself',
  'Give the child commands about their own body: "touch your right knee with your left hand", "your left ear with your right hand", "your left shoulder with your left hand". Start with simple ones, then add crossing the midline. Go slowly and let them think. This is the foundation for all later spatial concepts.' FROM new_game;

-- 3.3 Роботик / Robot
WITH new_game AS (
  INSERT INTO game (zone_id, min_age_months, max_age_months, active, cooldown_days)
  SELECT id, 48, 72, true, NULL FROM zone WHERE code='spatial'
  RETURNING id
)
INSERT INTO game_translation (game_id, locale, title, instructions)
SELECT id, 'UA', 'Роботик',
  'Дитина — робот і рухається лише за командами: «два кроки вперед», «поворот ліворуч», «крок назад», «стоп». Задайте ціль — наприклад, дійти до дверей. Потім поміняйтесь ролями: команди дає дитина, а ви виконуєте буквально (це дуже смішно і вчить точності). Тренує орієнтацію в напрямках і планування маршруту.' FROM new_game
UNION ALL
SELECT id, 'en', 'Robot',
  'The child is a robot and moves only on command: "two steps forward", "turn left", "one step back", "stop". Set a goal — for example, reaching the door. Then swap roles: the child gives the commands and you follow them literally (this is very funny and teaches precision). Trains direction awareness and route planning.' FROM new_game;

-- 3.4 Де я сховав? / Where did I hide it?
WITH new_game AS (
  INSERT INTO game (zone_id, min_age_months, max_age_months, active, cooldown_days)
  SELECT id, 36, 72, true, NULL FROM zone WHERE code='spatial'
  RETURNING id
)
INSERT INTO game_translation (game_id, locale, title, instructions)
SELECT id, 'UA', 'Де я сховав?',
  'Заховайте будь-яку іграшку в кімнаті й наводьте дитину лише словами про простір: «під столом», «за кріслом», «на полиці зверху», «між подушками». Не показуйте рукою — тільки слова. Потім міняйтесь: ховає дитина й пояснює вам. Тренує розуміння прийменників простору.' FROM new_game
UNION ALL
SELECT id, 'en', 'Where did I hide it?',
  'Hide any toy in the room and guide the child using spatial words only: "under the table", "behind the armchair", "on the top shelf", "between the cushions". Do not point — words only. Then swap: the child hides something and explains to you. Trains understanding of spatial prepositions.' FROM new_game;

-- 3.5 Дзеркальні рухи / Mirror directions
WITH new_game AS (
  INSERT INTO game (zone_id, min_age_months, max_age_months, active, cooldown_days)
  SELECT id, 60, 72, true, NULL FROM zone WHERE code='spatial'
  RETURNING id
)
INSERT INTO game_translation (game_id, locale, title, instructions)
SELECT id, 'UA', 'Навпаки',
  'Станьте навпроти дитини. Домовтесь: усе треба робити ДЗЕРКАЛЬНО. Ви піднімаєте праву руку — дитина піднімає ліву (ту, що навпроти). Ви нахиляєтесь вліво — дитина вправо. Це складніше, ніж звичайне «Дзеркало», бо треба подумки перевернути простір. Для старших дітей.' FROM new_game
UNION ALL
SELECT id, 'en', 'The opposite way',
  'Stand facing the child. Agree that everything must be done as a MIRROR image. You raise your right hand — the child raises their left (the one opposite). You lean left — the child leans right. This is harder than plain "Mirror" because it requires mentally flipping space. For older children.' FROM new_game;

-- ============================================================
-- ZONE 4 — КРУПНА МОТОРИКА / GROSS MOTOR SKILLS
-- ============================================================

-- 4.1 Тварини / Animal walks
WITH new_game AS (
  INSERT INTO game (zone_id, min_age_months, max_age_months, active, cooldown_days)
  SELECT id, 36, 72, true, NULL FROM zone WHERE code='gross_motor'
  RETURNING id
)
INSERT INTO game_translation (game_id, locale, title, instructions)
SELECT id, 'UA', 'Ходою тварин',
  'Пройдіть кімнату по-різному: ведмедем (на долонях і ступнях), жабкою (навприсядки, стрибками), чаплею (на одній нозі), змійкою (повзком на животі), крабом (на руках і ногах спиною донизу). Кожну ходу — 5–10 кроків. Тренує велику моторику, силу і координацію всього тіла.' FROM new_game
UNION ALL
SELECT id, 'en', 'Animal walks',
  'Cross the room in different ways: as a bear (on hands and feet), a frog (squatting, jumping), a heron (on one leg), a snake (crawling on the tummy), a crab (on hands and feet, belly up). Do 5–10 steps of each. Trains gross motor skills, strength and whole-body coordination.' FROM new_game;

-- 4.2 Чапля / Heron balance
WITH new_game AS (
  INSERT INTO game (zone_id, min_age_months, max_age_months, active, cooldown_days)
  SELECT id, 36, 72, true, NULL FROM zone WHERE code='gross_motor'
  RETURNING id
)
INSERT INTO game_translation (game_id, locale, title, instructions)
SELECT id, 'UA', 'Чапля',
  'Станьте на одну ногу й тримайте рівновагу якомога довше. Рахуйте вголос разом. Потім на другу ногу. Ускладнення: заплющити очі, або тримати рівновагу й одночасно плескати в долоні. Тренує рівновагу й відчуття власного тіла.' FROM new_game
UNION ALL
SELECT id, 'en', 'Heron',
  'Stand on one leg and hold your balance as long as you can, counting out loud together. Then switch legs. To make it harder: close your eyes, or hold the balance while clapping. Trains balance and body awareness.' FROM new_game;

-- 4.3 Стрибки за командою / Jump on command
WITH new_game AS (
  INSERT INTO game (zone_id, min_age_months, max_age_months, active, cooldown_days)
  SELECT id, 36, 72, true, NULL FROM zone WHERE code='gross_motor'
  RETURNING id
)
INSERT INTO game_translation (game_id, locale, title, instructions)
SELECT id, 'UA', 'Стрибки за командою',
  'Домовтесь про напрямки: «вперед», «назад», «вправо», «вліво». Дорослий називає — дитина стрибає обома ногами в цей бік. Далі ускладнюйте: два стрибки поспіль («вперед-вперед»), або серію з трьох команд, яку треба запам''ятати й виконати. Тренує координацію, силу ніг і слухову пам''ять.' FROM new_game
UNION ALL
SELECT id, 'en', 'Jump on command',
  'Agree on the directions: "forward", "back", "right", "left". The adult calls one out and the child jumps that way with both feet. Then make it harder: two jumps in a row ("forward-forward"), or a series of three commands to remember and perform. Trains coordination, leg strength and auditory memory.' FROM new_game;

-- 4.4 Танці «Завмри» / Freeze dance
WITH new_game AS (
  INSERT INTO game (zone_id, min_age_months, max_age_months, active, cooldown_days)
  SELECT id, 36, 72, true, NULL FROM zone WHERE code='gross_motor'
  RETURNING id
)
INSERT INTO game_translation (game_id, locale, title, instructions)
SELECT id, 'UA', 'Завмри',
  'Танцюйте разом (можна просто наспівувати). Раптом дорослий каже «Завмри!» — і всі застигають у тій позі, в якій були. Тримайте паузу кілька секунд, потім «Відмерли!» — і танці тривають. Ускладнення: завмерти в позі конкретної тварини. Тренує керування тілом і гальмування руху.' FROM new_game
UNION ALL
SELECT id, 'en', 'Freeze',
  'Dance together (humming a tune is enough). Suddenly the adult says "Freeze!" and everyone holds the pose they were in. Hold for a few seconds, then "Unfreeze!" and the dancing continues. To make it harder: freeze in the shape of a particular animal. Trains body control and inhibition of movement.' FROM new_game;

-- 4.5 Велетень і гном / Giant and dwarf
WITH new_game AS (
  INSERT INTO game (zone_id, min_age_months, max_age_months, active, cooldown_days)
  SELECT id, 36, 72, true, NULL FROM zone WHERE code='gross_motor'
  RETURNING id
)
INSERT INTO game_translation (game_id, locale, title, instructions)
SELECT id, 'UA', 'Велетень і гном',
  'На слово «велетень» дитина витягується вгору навшпиньки з піднятими руками, на слово «гном» — присідає якнайнижче. Дорослий чергує слова, поступово пришвидшуючись. Ускладнення: дорослий каже «велетень», а сам присідає — дитина має слухати слова, а не дивитись. Тренує велику моторику й довільну увагу.' FROM new_game
UNION ALL
SELECT id, 'en', 'Giant and dwarf',
  'On the word "giant" the child stretches up on tiptoes with arms raised; on "dwarf" they squat down as low as they can. The adult alternates the words, gradually speeding up. To make it harder: say "giant" while squatting yourself — the child must follow the words, not what they see. Trains gross motor skills and voluntary attention.' FROM new_game;

-- ============================================================
-- ZONE 5 — ДРІБНА МОТОРИКА / FINE MOTOR SKILLS
-- ============================================================

-- 5.1 Кулак-ребро-долоня / Fist–edge–palm
WITH new_game AS (
  INSERT INTO game (zone_id, min_age_months, max_age_months, active, cooldown_days)
  SELECT id, 48, 72, true, NULL FROM zone WHERE code='fine_motor'
  RETURNING id
)
INSERT INTO game_translation (game_id, locale, title, instructions)
SELECT id, 'UA', 'Кулак — ребро — долоня',
  'Покажіть послідовність на столі або на коліні: кулак, потім ребро долоні, потім розкрита долоня. Повторюйте по колу, спочатку дуже повільно, промовляючи вголос. Далі — швидше й мовчки; потім другою рукою; найскладніше — обома руками одночасно. Класична вправа на послідовні рухи.' FROM new_game
UNION ALL
SELECT id, 'en', 'Fist — edge — palm',
  'Show the sequence on a table or on your knee: a fist, then the edge of the hand, then an open palm. Repeat in a loop, first very slowly and saying it out loud. Then faster and in silence; then with the other hand; hardest of all — both hands at once. A classic exercise for sequenced movement.' FROM new_game;

-- 5.2 Пальчики вітаються / Fingers say hello
WITH new_game AS (
  INSERT INTO game (zone_id, min_age_months, max_age_months, active, cooldown_days)
  SELECT id, 36, 72, true, NULL FROM zone WHERE code='fine_motor'
  RETURNING id
)
INSERT INTO game_translation (game_id, locale, title, instructions)
SELECT id, 'UA', 'Пальчики вітаються',
  'Великий палець по черзі «вітається» з кожним іншим пальцем тієї ж руки — торкається подушечка до подушечки: з вказівним, середнім, безіменним, мізинцем, і назад. Спочатку правою рукою, потім лівою, потім обома. Робіть повільно й чітко, з торканням, а не змахом. Тренує точність і диференціацію пальців.' FROM new_game
UNION ALL
SELECT id, 'en', 'Fingers say hello',
  'The thumb greets each of the other fingers on the same hand in turn — pad to pad: index, middle, ring, little finger, and back again. First with the right hand, then the left, then both. Go slowly and precisely, with a real touch rather than a flick. Trains finger precision and differentiation.' FROM new_game;

-- 5.3 Замок / The lock
WITH new_game AS (
  INSERT INTO game (zone_id, min_age_months, max_age_months, active, cooldown_days)
  SELECT id, 36, 72, true, NULL FROM zone WHERE code='fine_motor'
  RETURNING id
)
INSERT INTO game_translation (game_id, locale, title, instructions)
SELECT id, 'UA', 'Замок',
  'Складіть пальці обох рук у «замок». Тепер, не розчіплюючи, поворушіть по черзі кожною парою пальців: спочатку великими, потім вказівними, і так до мізинців. Далі — «замок» навпаки: розчепіть і складіть так, щоб зверху був інший великий палець (це несподівано незвично). Тренує силу і чутливість пальців.' FROM new_game
UNION ALL
SELECT id, 'en', 'The lock',
  'Interlace the fingers of both hands into a "lock". Without unlocking, wiggle each pair of fingers in turn: first the thumbs, then the index fingers, and so on to the little fingers. Then make the "lock" the other way round: unlace and re-lace so that the other thumb ends up on top (it feels surprisingly odd). Trains finger strength and sensitivity.' FROM new_game;

-- 5.4 Тіньовий театр руками / Hand shadows
WITH new_game AS (
  INSERT INTO game (zone_id, min_age_months, max_age_months, active, cooldown_days)
  SELECT id, 48, 72, true, NULL FROM zone WHERE code='fine_motor'
  RETURNING id
)
INSERT INTO game_translation (game_id, locale, title, instructions)
SELECT id, 'UA', 'Театр рук',
  'Складайте з пальців фігурки й вгадуйте їх: зайчик (два пальці вгору), пташка (долоні схрещені, махають), собачка, равлик, коза. Дорослий показує — дитина вгадує й повторює, потім навпаки. Не потрібне світло чи стіна: досить самих рук. Тренує точні координовані рухи пальців.' FROM new_game
UNION ALL
SELECT id, 'en', 'Hand theatre',
  'Make figures with your fingers and guess them: a bunny (two fingers up), a bird (crossed palms flapping), a dog, a snail, a goat. The adult shows one, the child guesses and copies it, then swap. No lamp or wall needed — the hands are enough. Trains precise, coordinated finger movements.' FROM new_game;

-- 5.5 Невидиме письмо / Invisible writing
WITH new_game AS (
  INSERT INTO game (zone_id, min_age_months, max_age_months, active, cooldown_days)
  SELECT id, 48, 72, true, NULL FROM zone WHERE code='fine_motor'
  RETURNING id
)
INSERT INTO game_translation (game_id, locale, title, instructions)
SELECT id, 'UA', 'Невидиме письмо',
  'Малюйте пальцем на долоні дитини просту фігуру — коло, хрестик, квадрат, хвильку — а вона вгадує із заплющеними очима. Потім міняйтесь. Для старших: малюйте літери чи цифри. Тренує чутливість пальців і тактильне сприйняття.' FROM new_game
UNION ALL
SELECT id, 'en', 'Invisible writing',
  'Draw a simple shape with your finger on the child''s palm — a circle, a cross, a square, a wavy line — and they guess it with their eyes closed. Then swap. For older children: draw letters or numbers. Trains finger sensitivity and tactile perception.' FROM new_game;

-- 5.6 Вісімки в повітрі / Figure eights in the air
WITH new_game AS (
  INSERT INTO game (zone_id, min_age_months, max_age_months, active, cooldown_days)
  SELECT id, 48, 72, true, NULL FROM zone WHERE code='fine_motor'
  RETURNING id
)
INSERT INTO game_translation (game_id, locale, title, instructions)
SELECT id, 'UA', 'Вісімки в повітрі',
  'Витягніть руку вперед і намалюйте в повітрі велику лежачу вісімку — плавно, стежачи за рукою очима. Потім другою рукою. Далі — обома руками одночасно, дзеркально. Рухи мають бути повільними й неперервними. Тренує плавність рухів і зорово-моторну координацію.' FROM new_game
UNION ALL
SELECT id, 'en', 'Figure eights in the air',
  'Stretch out one arm and draw a large sideways figure eight in the air — smoothly, following your hand with your eyes. Then the other arm. Then both arms at once, mirroring each other. The movement should be slow and continuous. Trains smoothness of movement and hand–eye coordination.' FROM new_game;

-- ============================================================
-- SANITY CHECKS (run after seeding)
-- ============================================================
-- Games per zone:
-- SELECT z.code, COUNT(g.id)
-- FROM zone z LEFT JOIN game g ON g.zone_id = z.id
-- GROUP BY z.code ORDER BY z.code;
--
-- Every game must have exactly 2 translations (UA + en):
-- SELECT g.id, COUNT(gt.id) AS translations
-- FROM game g LEFT JOIN game_translation gt ON gt.game_id = g.id
-- GROUP BY g.id HAVING COUNT(gt.id) <> 2;
--
-- Localized listing (change 'UA' to 'en'):
-- SELECT zt.name AS zone, gt.title AS game
-- FROM game g
-- JOIN zone z ON z.id = g.zone_id
-- JOIN zone_translation zt ON zt.zone_id = z.id AND zt.locale = 'UA'
-- JOIN game_translation gt ON gt.game_id = g.id AND gt.locale = 'UA'
-- ORDER BY zt.name, gt.title;
