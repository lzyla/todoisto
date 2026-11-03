-- Todoisto: kontrola bezpieczeństwa. Uruchom w SQL Editor; NIC nie zmienia.
-- Oczekiwany wynik: każda tabela todoisto_* ma rls_enabled = true
-- (rls_forced = true dla todoisto_backups; historia celowo bez FORCE),
-- a w drugim zapytaniu wszystkie polityki mają roles = {authenticated} i warunek z auth.uid().

select c.relname                as table_name,
       c.relrowsecurity         as rls_enabled,
       c.relforcerowsecurity    as rls_forced
  from pg_class c
  join pg_namespace n on n.oid = c.relnamespace
 where n.nspname = 'public' and c.relkind = 'r' and c.relname like 'todoisto%'
 order by 1;

select tablename, policyname, cmd, roles, qual, with_check
  from pg_policies
 where schemaname = 'public' and tablename like 'todoisto%'
 order by tablename, policyname;

-- Uprawnienia roli anon (powinno być PUSTE):
select table_name, privilege_type
  from information_schema.role_table_grants
 where grantee = 'anon' and table_schema = 'public' and table_name like 'todoisto%';
