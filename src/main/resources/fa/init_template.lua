-- TODO this file can surely be reduced, but for now I don't want to risk breaking anything so I keep it as-is.
fa_path = '((fa_path))'

path = {}
blacklist = {
    "00_BigMap.scd",
    "00_BigMapLEM.scd",
    "fa-ladder.scd",
    "fa_ladder.scd",
    "faladder.scd",
    "powerlobby.scd",
    "02_sorian_ai_pack.scd",
    "03_lobbyenhancement.scd",
    "randmap.scd",
    "_Eject.scd",
    "Eject.scd",
    "gaz_ui",
    "lobby.nxt",
    "faforever.nxt"
}
whitelist =
{
    "effects.nx2",
    "env.nx2",
    "etc.nx2",
    "loc.nx2",
    "lua.nx2",
    "meshes.nx2",
    "mods.nx2",
    "modules.nx2",
    "projectiles.nx2",
    "schook.nx2",
    "textures.nx2",
    "units.nx2",
    "murderparty.nxt",
    "labwars.nxt",
    "units.scd",
    "textures.scd",
    "skins.scd",
    "schook.scd",
    "props.scd",
    "projectiles.scd",
    "objects.scd",
    "moholua.scd",
    "mohodata.scd",
    "mods.scd",
    "meshes.scd",
    "lua.scd",
    "loc_us.scd",
    "loc_es.scd",
    "loc_fr.scd",
    "loc_it.scd",
    "loc_de.scd",
    "loc_ru.scd",
    "env.scd",
    "effects.scd",
    "editor.scd",
    "ambience.scd",
    "advanced strategic icons.nxt",
    "lobbymanager_v105.scd",
    "texturepack.nxt",
    "sc_music.scd"
}

local function mount_dir(dir, mountpoint)
    table.insert(path, { dir = dir, mountpoint = mountpoint })
end

local function mount_contents(dir, mountpoint)
    LOG('Checking ' .. dir)
    for _, entry in io.dir(dir .. '\\*') do
        if entry ~= '.' and entry ~= '..' then
            local mp = string.lower(entry)
            local safe = true
            for i, black in blacklist do
                safe = safe and (string.find(mp, black, 1) == nil)
            end
            if safe then
                mp = string.gsub(mp, '[.]scd$', '')
                mp = string.gsub(mp, '[.]zip$', '')
                mount_dir(dir .. '\\' .. entry, mountpoint .. '/' .. mp)
            else
                LOG('Blacklisted: ' .. entry)
            end
        end
    end
end

local function mount_dir_with_whitelist(dir, glob)
    local sorted = {}
    LOG('Checking ' .. dir .. glob)
    for _, entry in io.dir(dir .. glob) do
        if entry ~= '.' and entry ~= '..' then
            local mp = string.lower(entry)
            local notsafe = true
            for i, white in whitelist do
                notsafe = notsafe and (string.find(mp, white, 1) == nil)
            end
            if notsafe then
                LOG('Not in whitelist: ' .. dir .. entry)
            else
                table.insert(sorted, dir .. entry)
            end
        end
    end
    table.sort(sorted)
    table.foreach(sorted, function(k, v) mount_dir(v, '/') end)
end

local function mount_dir_with_blacklist(dir, glob)
    local sorted = {}
    LOG('Checking ' .. dir .. glob)
    for _, entry in io.dir(dir .. glob) do
        if entry ~= '.' and entry ~= '..' then
            local mp = string.lower(entry)
            local safe = true
            for i, black in blacklist do
                safe = safe and (string.find(mp, black, 1) == nil)
            end
            if safe then
                table.insert(sorted, dir .. entry)
            else
                LOG('Blacklisted: ' .. dir .. entry)
            end
        end
    end
    table.sort(sorted)
    table.foreach(sorted, function(k, v) mount_dir(v, '/') end)
end

local function mount_map_dir(dir, glob, mountpoint)
    LOG('mounting maps from: ' .. dir)
    mount_contents(dir, mountpoint)
    for _, map in io.dir(dir .. glob) do
        for _, folder in io.dir(dir .. '\\' .. map .. '\\**') do
            if folder == 'movies' then
                LOG('Found map movies in: ' .. map)
                mount_dir(dir .. map .. '\\movies', '/movies')
            elseif folder == 'sounds' then
                LOG('Found map sounds in: ' .. map)
                mount_dir(dir .. map .. '\\sounds', '/sounds')
            end
        end
    end
end

local function mount_mod_sounds(modFolder)
    -- searching for mods inside the modfolder
    for _, mod in io.dir(modFolder .. '\\*.*') do
        -- do we have a true directory ?
        if mod ~= '.' and mod ~= '..' then
            -- searching for sounds inside mod folder
            for _, folder in io.dir(modFolder .. '\\' .. mod .. '\\*.*') do
                -- if we found a folder named sounds then mount it
                if folder == 'sounds' then
                    LOG('Found mod sounds in: ' .. mod)
                    mount_dir(modFolder .. '\\' .. mod .. '\\sounds', '/sounds')
                    break
                end
            end
        end
    end
end

-- Begin mod mounting section
-- This section mounts sounds from the mods directory to allow mods to add custom sounds to the game

mount_map_dir(SHGetFolderPath('PERSONAL') .. 'My Games\\Gas Powered Games\\Supreme Commander Forged Alliance\\maps\\', '**', '/maps')
mount_map_dir(InitFileDir .. '\\..\\user\\My Games\\Gas Powered Games\\Supreme Commander Forged Alliance\\maps\\', '**', '/maps')

mount_mod_sounds(SHGetFolderPath('PERSONAL') .. 'My Games\\Gas Powered Games\\Supreme Commander Forged Alliance\\mods')
mount_mod_sounds(InitFileDir .. '\\..\\user\\My Games\\Gas Powered Games\\Supreme Commander Forged Alliance\\mods')
-- End mod mounting section

-- these are the classic supcom directories. They don't work with accents or other foreign characters in usernames
mount_contents(SHGetFolderPath('PERSONAL') .. 'My Games\\Gas Powered Games\\Supreme Commander Forged Alliance\\mods', '/mods')
mount_contents(SHGetFolderPath('PERSONAL') .. 'My Games\\Gas Powered Games\\Supreme Commander Forged Alliance\\maps', '/maps')

-- these are the local FAF directories. The My Games ones are only there for people with usernames that don't work in the uppder ones.
mount_contents(InitFileDir .. '\\..\\user\\My Games\\Gas Powered Games\\Supreme Commander Forged Alliance\\mods', '/mods')
mount_contents(InitFileDir .. '\\..\\user\\My Games\\Gas Powered Games\\Supreme Commander Forged Alliance\\maps', '/maps')
mount_dir_with_whitelist(InitFileDir .. '\\..\\gamedata\\', '*.nxt', '/')
mount_dir_with_whitelist(InitFileDir .. '\\..\\gamedata\\', '*.nx2', '/')


for _, dir in { ((mount_dirs)) } do
    mount_dir(dir, '/')
end


-- these are using the newly generated path from the dofile() statement at the beginning of this script
mount_dir_with_whitelist(fa_path .. '\\gamedata\\', '*.scd', '/')
mount_dir(fa_path, '/')

hook = {
    '/schook'
}

protocols = {
    'http',
    'https',
    'mailto',
    'ventrilo',
    'teamspeak',
    'daap',
    'im',
}
