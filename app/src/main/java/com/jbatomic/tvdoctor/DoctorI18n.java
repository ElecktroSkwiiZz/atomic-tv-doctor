package com.jbatomic.tvdoctor;

import java.util.HashMap;
import java.util.Map;

/** Centralized UI strings. French is the canonical key/fallback language. */
public final class DoctorI18n {
    private DoctorI18n(){}
    private static final Map<String,String[]> words=new HashMap<>();
    private static void add(String fr,String en,String es,String ru){
        words.put(fr.toLowerCase(java.util.Locale.ROOT),new String[]{fr,en,es,ru});
    }
    static {
        add("Vue d'ensemble","Overview","Resumen","Обзор");
        add("Applications","Applications","Aplicaciones","Приложения");
        add("Maintenance","Maintenance","Mantenimiento","Обслуживание");
        add("Fichiers","Files","Archivos","Файлы");
        add("Technique","Technical","Información técnica","Технические данные");
        add("Portail PC","PC portal","Portal PC","Веб-портал");
        add("Mises à jour","Updates","Actualizaciones","Обновления");
        add("Tableau de bord","Dashboard","Panel de control","Панель управления");
        add("Système","System","Sistema","Система");
        add("Mémoire RAM","RAM memory","Memoria RAM","Оперативная память");
        add("Stockage interne","Internal storage","Almacenamiento interno","Хранилище");
        add("Vidéo et affichage","Video & display","Vídeo y pantalla","Видео и экран");
        add("Réseau","Network","Red","Сеть");
        add("Décodage multimédia","Media decoding","Decodificación multimedia","Декодирование медиа");
        add("Applications détectées","Detected apps","Aplicaciones detectadas","Найденные приложения");
        add("MESURES LOCALES","LOCAL DATA","DATOS LOCALES","ЛОКАЛЬНЫЕ ДАННЫЕ");
        add("Actualiser","Refresh","Actualizar","Обновить");
        add("Rapport","Report","Informe","Отчёт");
        add("Réglages","Settings","Configuración","Настройки");
        add("Langue","Language","Idioma","Язык");
        add("Outils avancés","Advanced tools","Herramientas avanzadas","Расширенные инструменты");
        add("NETTOYAGE STANDARD","STANDARD CLEANUP","LIMPIEZA ESTÁNDAR","СТАНДАРТНАЯ ОЧИСТКА");
        add("Nettoyage sûr","Safe cleanup","Limpieza segura","Безопасная очистка");
        add("Nettoyer les fichiers temporaires","Clear temporary files","Limpiar archivos temporales","Очистить временные файлы");
        add("APPLICATIONS","APPLICATIONS","APLICACIONES","ПРИЛОЖЕНИЯ");
        add("Gérer depuis Doctor","Manage in Doctor","Gestionar en Doctor","Управлять в Doctor");
        add("Gestionnaire des applications","Application manager","Gestor de aplicaciones","Менеджер приложений");
        add("RAPPORT ET DIAGNOSTIC","REPORT AND DIAGNOSTICS","INFORME Y DIAGNÓSTICO","ОТЧЁТ И ДИАГНОСТИКА");
        add("Analyse détaillée","Detailed analysis","Análisis detallado","Подробный анализ");
        add("Exporter le rapport JSON","Export JSON report","Exportar informe JSON","Экспорт JSON-отчёта");
        add("ACCÈS PRIVILÉGIÉ","PRIVILEGED ACCESS","ACCESO PRIVILEGIADO","ПРИВИЛЕГИРОВАННЫЙ ДОСТУП");
        add("ROOT REQUIS","ROOT REQUIRED","REQUIERE ROOT","ТРЕБУЕТСЯ ROOT");
        add("Vérifier les droits ROOT","Check ROOT access","Comprobar ROOT","Проверить ROOT");
        add("CACHES ANDROID","ANDROID CACHES","CACHÉS DE ANDROID","КЭШ ANDROID");
        add("Purger les caches système","Clear system caches","Limpiar cachés del sistema","Очистить системный кэш");
        add("CONTRÔLE D'APPLICATIONS","APP CONTROL","CONTROL DE APPS","УПРАВЛЕНИЕ ПРИЛОЖЕНИЯМИ");
        add("Choisir une application","Choose an app","Elegir una aplicación","Выбрать приложение");
        add("Vérifier ROOT","Check ROOT","Comprobar ROOT","Проверить ROOT");
        add("ROOT non vérifié","ROOT not checked","ROOT sin comprobar","ROOT не проверен");
        add("ROOT AUTORISÉ","ROOT GRANTED","ROOT AUTORIZADO","ROOT РАЗРЕШЁН");
        add("ROOT INDISPONIBLE","ROOT UNAVAILABLE","ROOT NO DISPONIBLE","ROOT НЕДОСТУПЕН");
        add("ROOT non disponible ou refusé","ROOT unavailable or denied","ROOT no disponible o denegado","ROOT недоступен или отклонён");
        add("Gestionnaire d'applications","Application manager","Gestor de aplicaciones","Менеджер приложений");
        add("Ouvrir","Open","Abrir","Открыть");
        add("Supprimer","Uninstall","Desinstalar","Удалить");
        add("Arrêter","Stop","Detener","Остановить");
        add("Geler","Freeze","Congelar","Заморозить");
        add("Réactiver","Re-enable","Reactivar","Включить снова");
        add("Applications gelées","Frozen apps","Aplicaciones congeladas","Замороженные приложения");
        add("Diagnostic technique","Technical diagnostic","Diagnóstico técnico","Техническая диагностика");
        add("SYSTÈME ET SOC","SYSTEM AND SOC","SISTEMA Y SOC","СИСТЕМА И SOC");
        add("RAM ET STOCKAGE","RAM AND STORAGE","RAM Y ALMACENAMIENTO","ОЗУ И ХРАНИЛИЩЕ");
        add("AFFICHAGE ET AUDIO","DISPLAY AND AUDIO","PANTALLA Y AUDIO","ЭКРАН И ЗВУК");
        add("RÉSEAU & CODECS","NETWORK & CODECS","RED Y CÓDECS","СЕТЬ И КОДЕКИ");
        add("Partager le rapport JSON complet","Share full JSON report","Compartir informe JSON completo","Поделиться JSON-отчётом");
        add("Dalle native","Native panel","Panel nativo","Матрица");
        add("Résolution de la dalle","Panel resolution","Resolución del panel","Разрешение матрицы");
        add("Définir la résolution de dalle","Set panel resolution","Definir resolución del panel","Задать разрешение матрицы");
        add("Non déterminée","Not determined","No determinada","Не определено");
        add("4K UHD (3840 × 2160)","4K UHD (3840 × 2160)","4K UHD (3840 × 2160)","4K UHD (3840 × 2160)");
        add("Full HD (1920 × 1080)","Full HD (1920 × 1080)","Full HD (1920 × 1080)","Full HD (1920 × 1080)");
        add("Autre / non déterminée","Other / unknown","Otra / desconocida","Другое / неизвестно");
        add("Surface Android","Android UI surface","Superficie Android","Интерфейс Android");
        add("Rendu Android","Android rendering","Renderizado Android","Рендеринг Android");
        add("Déclarée par l'utilisateur","User specified","Declarada por el usuario","Указано пользователем");
        add("Portail PC","PC portal","Portal PC","Веб-портал");
        add("CONNEXION LOCALE","LOCAL CONNECTION","CONEXIÓN LOCAL","ЛОКАЛЬНОЕ СОЕДИНЕНИЕ");
        add("Activer le portail PC","Enable PC portal","Activar portal PC","Включить веб-портал");
        add("Désactiver le portail","Disable portal","Desactivar portal","Выключить портал");
        add("Port HTTP","HTTP port","Puerto HTTP","Порт HTTP");
        add("Mises à jour","Updates","Actualizaciones","Обновления");
        add("CANAL STABLE","STABLE CHANNEL","CANAL ESTABLE","СТАБИЛЬНЫЙ КАНАЛ");
        add("Vérifier les mises à jour","Check for updates","Buscar actualizaciones","Проверить обновления");
        add("Gestionnaire de fichiers","File manager","Gestor de archivos","Файловый менеджер");
        add("Choisir un dossier","Choose a folder","Elegir una carpeta","Выбрать папку");
        add("Changer de dossier","Change folder","Cambiar carpeta","Сменить папку");
        add("Dossier parent","Parent folder","Carpeta superior","Родительская папка");
        add("Trier par taille","Sort by size","Ordenar por tamaño","Сортировать по размеру");
        add("Trier par nom","Sort by name","Ordenar por nombre","Сортировать по имени");
        add("Nettoyer le stockage","Clean up storage","Limpiar almacenamiento","Очистить хранилище");
        add("Gestionnaire de fichiers et nettoyage","File manager and cleanup","Gestor de archivos y limpieza","Файловый менеджер и очистка");
        add("Dossier","Folder","Carpeta","Папка");
        add("Fichier","File","Archivo","Файл");
        add("Vider la RAM","Free RAM","Liberar RAM","Освободить ОЗУ");
        add("MÉMOIRE ET PERFORMANCES","MEMORY AND PERFORMANCE","MEMORIA Y RENDIMIENTO","ПАМЯТЬ И ПРОИЗВОДИТЕЛЬНОСТЬ");
        add("Libérer la RAM inutilisée","Release background RAM","Liberar RAM en segundo plano","Освободить фоновую ОЗУ");
        add("Mesurer avant et après","Measure before and after","Medir antes y después","Измерить до и после");
        add("Annuler","Cancel","Cancelar","Отмена");
        add("Confirmer","Confirm","Confirmar","Подтвердить");
        add("Supprimer le fichier","Delete file","Eliminar archivo","Удалить файл");
        add("Supprimer le dossier","Delete folder","Eliminar carpeta","Удалить папку");
        add("Fichiers volumineux","Large files","Archivos grandes","Крупные файлы");
        add("Dossier autorisé","Authorized folder","Carpeta autorizada","Разрешённая папка");
        add("Stockage","Storage","Almacenamiento","Хранилище");
    }
    public static String translate(String lang,String text){
        int index="en".equals(lang)?1:"es".equals(lang)?2:"ru".equals(lang)?3:0;
        if(index==0||text==null)return text;
        String[] exact=words.get(text.toLowerCase(java.util.Locale.ROOT));
        return exact==null?text:exact[index];
    }
}
