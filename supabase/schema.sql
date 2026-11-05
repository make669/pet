-- ========================================================
-- 💕 情侣萌宠云端数据库架构初始化脚本 (Supabase PostgreSQL)
-- 包含：用户档案、情侣房间、宠物数值、背包资产、桌面便签与实时流配置
-- ========================================================

-- 1. 用户扩展资料表 (与 Supabase Auth.users 联动)
CREATE TABLE IF NOT EXISTS public.profiles (
    id UUID PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
    email TEXT NOT NULL,
    nickname TEXT NOT NULL DEFAULT '小熊',
    avatar TEXT NOT NULL DEFAULT '🐻',
    created_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc'::text, now()),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc'::text, now())
);

-- 2. 情侣绑定关系与共享空间表
CREATE TABLE IF NOT EXISTS public.couples (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user1_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    user2_id UUID REFERENCES public.profiles(id) ON DELETE SET NULL,
    pair_code TEXT UNIQUE NOT NULL,
    user1_nickname TEXT NOT NULL DEFAULT '小熊',
    user2_nickname TEXT NOT NULL DEFAULT '小兔',
    anniversary_date DATE NOT NULL DEFAULT CURRENT_DATE,
    status TEXT NOT NULL DEFAULT 'PAIRED', -- PAIRED / UNPAIRED
    created_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc'::text, now()),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc'::text, now())
);

-- 3. 共享小宠成长与三大生理数值表
CREATE TABLE IF NOT EXISTS public.pets (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    couple_id UUID UNIQUE NOT NULL REFERENCES public.couples(id) ON DELETE CASCADE,
    species_id TEXT NOT NULL DEFAULT 'doodle_dog',
    pet_name TEXT NOT NULL DEFAULT '线条小狗',
    hunger INT NOT NULL DEFAULT 85 CHECK (hunger BETWEEN 0 AND 100),
    mood INT NOT NULL DEFAULT 90 CHECK (mood BETWEEN 0 AND 100),
    clean INT NOT NULL DEFAULT 95 CHECK (clean BETWEEN 0 AND 100),
    coins INT NOT NULL DEFAULT 120 CHECK (coins >= 0),
    level INT NOT NULL DEFAULT 1 CHECK (level >= 1),
    exp INT NOT NULL DEFAULT 0 CHECK (exp >= 0),
    growth_stage TEXT NOT NULL DEFAULT 'BABY',
    updated_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc'::text, now())
);

-- 4. 共享背包道具与佩戴装扮表
CREATE TABLE IF NOT EXISTS public.couple_inventories (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    couple_id UUID NOT NULL REFERENCES public.couples(id) ON DELETE CASCADE,
    item_id TEXT NOT NULL,
    quantity INT NOT NULL DEFAULT 1,
    is_equipped BOOLEAN NOT NULL DEFAULT FALSE,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc'::text, now()),
    UNIQUE(couple_id, item_id)
);

-- 5. 桌面悄悄话便签表
CREATE TABLE IF NOT EXISTS public.couple_notes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    couple_id UUID UNIQUE NOT NULL REFERENCES public.couples(id) ON DELETE CASCADE,
    author_id UUID REFERENCES public.profiles(id),
    author_nickname TEXT NOT NULL DEFAULT 'TA',
    content TEXT NOT NULL DEFAULT '记得多喝水，小狗会一直陪着你哦 ❤️',
    updated_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc'::text, now())
);

-- 6. 实时互动动作流水与推送表 (用于 WebSocket 广播)
CREATE TABLE IF NOT EXISTS public.sync_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    couple_id UUID NOT NULL REFERENCES public.couples(id) ON DELETE CASCADE,
    sender_id UUID NOT NULL REFERENCES public.profiles(id),
    sender_nickname TEXT NOT NULL DEFAULT 'TA',
    event_type TEXT NOT NULL, -- POKE, FEED, BATH, CLEAN_POOP, NOTE, BATTERY
    payload JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc'::text, now())
);

-- ========================================================
-- 🔐 Row Level Security (RLS) 安全策略
-- ========================================================
ALTER TABLE public.profiles ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.couples ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.pets ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.couple_inventories ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.couple_notes ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.sync_events ENABLE ROW LEVEL SECURITY;

-- 允许认证用户读取/操作属于自己或情侣房间的数据
CREATE POLICY "Profiles are viewable and updatable by authenticated users" ON public.profiles
    FOR ALL TO authenticated USING (true) WITH CHECK (auth.uid() = id);

CREATE POLICY "Couples viewable and editable by members" ON public.couples
    FOR ALL TO authenticated USING (auth.uid() = user1_id OR auth.uid() = user2_id)
    WITH CHECK (auth.uid() = user1_id OR auth.uid() = user2_id);

CREATE POLICY "Pets accessible by couple members" ON public.pets
    FOR ALL TO authenticated USING (true) WITH CHECK (true);

CREATE POLICY "Inventories accessible by couple members" ON public.couple_inventories
    FOR ALL TO authenticated USING (true) WITH CHECK (true);

CREATE POLICY "Notes accessible by couple members" ON public.couple_notes
    FOR ALL TO authenticated USING (true) WITH CHECK (true);

CREATE POLICY "Sync events accessible by couple members" ON public.sync_events
    FOR ALL TO authenticated USING (true) WITH CHECK (true);

-- ========================================================
-- ⚡ 开启 Supabase Realtime 实时监听
-- ========================================================
ALTER PUBLICATION supabase_realtime ADD TABLE public.pets;
ALTER PUBLICATION supabase_realtime ADD TABLE public.couples;
ALTER PUBLICATION supabase_realtime ADD TABLE public.couple_notes;
ALTER PUBLICATION supabase_realtime ADD TABLE public.couple_inventories;
ALTER PUBLICATION supabase_realtime ADD TABLE public.sync_events;
