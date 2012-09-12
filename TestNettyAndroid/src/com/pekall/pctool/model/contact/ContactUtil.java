
package com.pekall.pctool.model.contact;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.net.Uri;
import android.os.RemoteException;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.provider.ContactsContract.CommonDataKinds.GroupMembership;
import android.provider.ContactsContract.CommonDataKinds.Im;
import android.provider.ContactsContract.CommonDataKinds.Nickname;
import android.provider.ContactsContract.CommonDataKinds.Organization;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.CommonDataKinds.Photo;
import android.provider.ContactsContract.CommonDataKinds.StructuredName;
import android.provider.ContactsContract.CommonDataKinds.StructuredPostal;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.Groups;
import android.provider.ContactsContract.RawContacts;
import android.telephony.PhoneNumberUtils;
import android.text.TextUtils;

import com.pekall.pctool.Slog;
import com.pekall.pctool.model.account.AccountInfo;
import com.pekall.pctool.model.contact.Contact.AddressInfo;
import com.pekall.pctool.model.contact.Contact.ContactVersion;
import com.pekall.pctool.model.contact.Contact.DataModel;
import com.pekall.pctool.model.contact.Contact.EmailInfo;
import com.pekall.pctool.model.contact.Contact.ImInfo;
import com.pekall.pctool.model.contact.Contact.ModifyTag;
import com.pekall.pctool.model.contact.Contact.OrgInfo;
import com.pekall.pctool.model.contact.Contact.PhoneInfo;
import com.pekall.pctool.model.contact.Contact.RawContact;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ContactUtil {

    private static final boolean DUMP_PARAMS = true;

    /****** Account ***********************************/
    public static final String DEFAULT_ACCOUNT_NAME = "contacts.account.name.local";
    public static final String DEFAULT_ACCOUNT_TYPE = "contacts.account.type.local";
    public static final String SIM1_ACCOUNT_NAME = "contacts.account.name.sim1";
    public static final String SIM1_ACCOUNT_TYPE = "contacts.account.type.sim";
    public static final String SIM2_ACCOUNT_NAME = "contacts.account.name.sim2";
    public static final String SIM2_ACCOUNT_TYPE = "contacts.account.type.sim";

    /******* Flag ******************************/
    private static final int DELETE_FLAG = 1;
    public static final int CONTACTS_COLLECT_FLAG = 1;

    /**
     * This utility class cannot be instantiated
     */
    private ContactUtil() {
    }
    
    /**
     * Get the {@link Contact} id by phone number 
     * 
     * @param context
     * @param number
     * @return {@link Contact} id or zero if cannot find the {@link Contact}
     */
    public static long getRawContactId(Context context, String number) {
        Cursor c = null;
        try {
            c = context.getContentResolver().query(Phone.CONTENT_URI,
                    new String[] {
                            Phone.RAW_CONTACT_ID, Phone.NUMBER
                    }, null, null, null);
            if (c != null && c.moveToFirst()) {
                while (!c.isAfterLast()) {
                    if (PhoneNumberUtils.compare(number, c.getString(/* Phone.NUMBER */ 1))) {
                        return c.getLong(/* Phone.RAW_CONTACT_ID */ 0);
                    }
                    c.moveToNext();
                }
            }
        } catch (Exception e) {
            Slog.e("getContactId error:", e);
        } finally {
            if (c != null) {
                c.close();
            }
        }
        return 0;
    }

    /**
     * getRawContactsByAccount
     * 
     * @param context
     * @param account
     * @return
     */
    public static List<RawContact> getRawContactsByAccount(Context context, AccountInfo account) {
        List<RawContact> lr = new ArrayList<RawContact>();
        if (account == null)
            return lr;
        String where = RawContacts.ACCOUNT_NAME + "=?" + " and " + RawContacts.ACCOUNT_TYPE + "=?" + "and "
                + RawContacts.DELETED + "!=" + DELETE_FLAG;
        String whereargs[] = new String[] {
                account.accountName, account.accountType
        };
        Cursor cursor = context.getContentResolver().query(RawContacts.CONTENT_URI, null, where, whereargs, null);
        if (cursor.moveToFirst()) {
            final int RAWCONTACTS_ID = cursor.getColumnIndex(RawContacts._ID);
            final int RAWCONTACTS_VERSION = cursor.getColumnIndex(RawContacts.VERSION);
            do {
                RawContact rw = new RawContact();
                rw.rawId = cursor.getLong(RAWCONTACTS_ID);
                rw.version = cursor.getInt(RAWCONTACTS_VERSION);
                rw.accountInfo.accountName = account.accountName;
                rw.accountInfo.accountType = account.accountType;
                lr.add(rw);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return lr;
    }

    /**
     * Get the specified contact version
     * 
     * @param context
     * @param id
     * @return
     */
    public static int getContactVersion(Context context, long id) {
        Cursor cursor = context.getContentResolver().query(RawContacts.CONTENT_URI, new String[] {
                RawContacts.VERSION
        }, RawContacts.DELETED + "!=? AND " + RawContacts._ID + "=?", new String[] {
                String.valueOf(DELETE_FLAG), String.valueOf(id)
        }, null);

        int version = -1;

        if (cursor != null) {
            if (cursor.moveToFirst()) {
                version = cursor.getInt(cursor.getColumnIndex(RawContacts.VERSION));
            }
            cursor.close();
        }
        return version;
    }

    /**
     * get all version of contacts
     * 
     * @param context
     * @return
     */
    public static List<ContactVersion> getAllContactVersions(Context context) {
        List<ContactVersion> contactVersions = new ArrayList<ContactVersion>();

        Cursor cursor = context.getContentResolver().query(RawContacts.CONTENT_URI, new String[] {
                RawContacts._ID, RawContacts.VERSION
        }, RawContacts.DELETED + "!=" + DELETE_FLAG, null, null);

        if (cursor != null) {
            if (cursor.moveToFirst()) {
                final int IndexOfId = cursor.getColumnIndex(RawContacts._ID);
                final int indexOfVersion = cursor.getColumnIndex(RawContacts.VERSION);

                do {
                    ContactVersion contactVersion = new ContactVersion();

                    contactVersion.id = cursor.getLong(IndexOfId);
                    contactVersion.version = cursor.getInt(indexOfVersion);

                    contactVersions.add(contactVersion);
                } while (cursor.moveToNext());
            }
            cursor.close();
        }
        return contactVersions;
    }

    /**
     * get AllGroupInfos
     * 
     * @param context
     * @return
     */
    public static List<GroupInfo> getAllGroups(Context context) {
        List<GroupInfo> li = new ArrayList<GroupInfo>();
        Cursor cursorOfGroup = context.getContentResolver().query(Groups.CONTENT_URI, null, null, null, null);
        if (cursorOfGroup.moveToFirst()) {
            final int GROUP_ID = cursorOfGroup.getColumnIndex(Groups._ID);
            final int GROUP_TITLE = cursorOfGroup.getColumnIndex(Groups.TITLE);
            final int GROUP_NOTES = cursorOfGroup.getColumnIndex(Groups.NOTES);
            final int GROUP_ACCOUNT_TYPE = cursorOfGroup.getColumnIndex(Groups.ACCOUNT_TYPE);
            final int GROUP_ACCOUNT_NAME = cursorOfGroup.getColumnIndex(Groups.ACCOUNT_NAME);
            do {
                GroupInfo gi = new GroupInfo();

                gi.grId = cursorOfGroup.getLong(GROUP_ID);
                gi.name = cursorOfGroup.getString(GROUP_TITLE);
                gi.note = cursorOfGroup.getString(GROUP_NOTES);
                gi.accountInfo.accountType = cursorOfGroup.getString(GROUP_ACCOUNT_TYPE);
                gi.accountInfo.accountName = cursorOfGroup.getString(GROUP_ACCOUNT_NAME);

                li.add(gi);

            } while (cursorOfGroup.moveToNext());
        }
        cursorOfGroup.close();
        return li;
    }

    /**
     * get some contacts of one account
     * 
     * @param context
     * @param accountName
     * @param accountType
     * @return
     */
    public static List<Contact> getContactsByAccount(Context context, AccountInfo account) {
        List<Contact> l = new ArrayList<Contact>();
        if (account == null) {
            return l;
        }
        List<RawContact> lr = getRawContactsByAccount(context, account);
        List<GroupInfo> li = getAllGroups(context);
        List<DataModel> ld = convertToDataModels(context, li);
        l = convertToContacts(lr, ld);
        return l;
    }

    /**
     * get all accounts of phone we need to show the account.name
     * 
     * @param context
     * @return
     */
    public static List<AccountInfo> getAllAccounts(Context context) {
        AccountManager accountManager = AccountManager.get(context);
        Account[] accounts = accountManager.getAccounts();
        List<AccountInfo> ls = new ArrayList<AccountInfo>();
        for (int i = 0; i < accounts.length; i++) {
            AccountInfo ai = new AccountInfo();
            ai.accountName = accounts[i].name;
            ai.accountType = accounts[i].type;
            ls.add(ai);
        }
        AccountInfo ai = new AccountInfo();
        ai.accountName = DEFAULT_ACCOUNT_NAME;
        ai.accountType = DEFAULT_ACCOUNT_TYPE;
        ls.add(ai);
        if (getSimCardState(context) == 1) {
            ai = new AccountInfo();
            ai.accountName = SIM1_ACCOUNT_NAME;
            ai.accountType = SIM1_ACCOUNT_TYPE;
            ls.add(ai);
        } else if (getSimCardState(context) == 2) {
            ai = new AccountInfo();
            ai.accountName = SIM2_ACCOUNT_NAME;
            ai.accountType = SIM2_ACCOUNT_TYPE;
            ls.add(ai);
        } else if (getSimCardState(context) == 3) {
            ai = new AccountInfo();
            ai.accountName = SIM1_ACCOUNT_NAME;
            ai.accountType = SIM1_ACCOUNT_TYPE;
            ls.add(ai);
            ai = new AccountInfo();
            ai.accountName = SIM2_ACCOUNT_NAME;
            ai.accountType = SIM2_ACCOUNT_TYPE;
            ls.add(ai);
        }
        return ls;
    }

    /**
     * get the sim card state return 1 means simcard 1 return 2 means simcard 2
     * return 3 means simcard1 and simcard2 return 0 means no simcard only test
     * for coolpal 7728
     * 
     * @param context
     * @return
     */
    public static int getSimCardState(Context context) {
        int count = 0;
        String where = RawContacts.ACCOUNT_NAME + "=?" + " and " + RawContacts.ACCOUNT_TYPE + "=?";
        String whereargs1[] = new String[] {
                SIM1_ACCOUNT_NAME, SIM1_ACCOUNT_TYPE
        };
        String whereargs2[] = new String[] {
                SIM2_ACCOUNT_NAME, SIM2_ACCOUNT_TYPE
        };
        Cursor cursor = context.getContentResolver().query(ContactsContract.RawContacts.CONTENT_URI, null, where,
                whereargs1, null);
        if (cursor.getCount() > 0)
            count++;
        cursor.close();
        cursor = context.getContentResolver().query(ContactsContract.RawContacts.CONTENT_URI, null, where, whereargs2,
                null);
        if (cursor.getCount() > 0)
            count += 2;
        cursor.close();
        return count;
    }

    /**
     * delete a contact if the contact has the same name
     * 
     * @param name context
     */
    public static boolean deleteContactByName(Context context, String name) {
        String projection[] = new String[] {
                Data.RAW_CONTACT_ID
        };
        String where = Contacts.DISPLAY_NAME + "=?";
        String whereargs[] = new String[] {
                name
        };
        Cursor cursor = context.getContentResolver().query(Data.CONTENT_URI, projection, where, whereargs, null);
        Uri rawContactUri = Uri.parse(RawContacts.CONTENT_URI.toString() + "?" + ContactsContract.CALLER_IS_SYNCADAPTER
                + "=true");
        ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();
        while (cursor.moveToNext()) {
            long Id = cursor.getLong(cursor.getColumnIndex(Data.RAW_CONTACT_ID));
            ops.add(ContentProviderOperation.newDelete(ContentUris.withAppendedId(rawContactUri, Id)).build());
        }
        cursor.close();
        try {
            context.getContentResolver().applyBatch(ContactsContract.AUTHORITY, ops);
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    /**
     * delete a contact by rawId
     * 
     * @param context ,id
     */
    public static boolean deleteContactById(Context context, long rawId) {
        ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();
        Uri rawContactUri = Uri.parse(RawContacts.CONTENT_URI.toString() + "?" + ContactsContract.CALLER_IS_SYNCADAPTER
                + "=true");
        ops.add(ContentProviderOperation.newDelete(ContentUris.withAppendedId(rawContactUri, rawId)).build());
        try {
            context.getContentResolver().applyBatch(ContactsContract.AUTHORITY, ops);
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    /**
     * delete some contact by rawId[]
     * 
     * @param context rawId[]
     */
    public static boolean deleteContactByIds(Context context, List<Long> rawIds) {
        ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();
        for (long rawId : rawIds) {
            ops.add(ContentProviderOperation.newDelete(ContentUris.withAppendedId(RawContacts.CONTENT_URI, rawId))
                    .build());
        }
        try {
            context.getContentResolver().applyBatch(ContactsContract.AUTHORITY, ops);
            return true;
        } catch (RemoteException e) {
            Slog.e("Error deleteContact", e);
            return false;
        } catch (OperationApplicationException e) {
            Slog.e("Error deleteContact", e);
            return false;
        }
    }

    public static int deleteContactAll(Context context) {
        return context.getContentResolver().delete(RawContacts.CONTENT_URI, null, null);
    }

    /**
     * Update Contact when there is no ModifyTag
     * 
     * @param context
     * @param contact
     * @return
     */
    public static boolean updateContactForce(Context context, Contact contact) {
        Slog.d("updateContactForce E");

        if (DUMP_PARAMS) {
            Slog.d(">>>>> DUMP CONTACT >>>>>");
            Slog.d(contact.toString());
            Slog.d("<<<<< DUMP CONTACT <<<<<");
        }

        ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();
        
        // update Name
        if (hasField(context, "name", contact.id)) {
            ops.add(ContentProviderOperation
                    .newUpdate(Data.CONTENT_URI)
                    .withSelection(Data.RAW_CONTACT_ID + "=?" + " AND " + Data.MIMETYPE + " = ?",
                            new String[] {
                                    String.valueOf(contact.id), StructuredName.CONTENT_ITEM_TYPE
                            }).withValue(StructuredName.DISPLAY_NAME, contact.name).build());
        } else {
            ops.add(ContentProviderOperation
                    .newInsert(Data.CONTENT_URI)
                    .withValue(Data.RAW_CONTACT_ID, contact.id)
                    .withValue(Data.MIMETYPE, StructuredName.CONTENT_ITEM_TYPE)
                    .withValue(StructuredName.DISPLAY_NAME, contact.name)
                    .withValue(StructuredName.FAMILY_NAME, "")
                    .withValue(StructuredName.GIVEN_NAME, "")
                    .withValue(StructuredName.MIDDLE_NAME, "").build());
        }
        
        // update nickname
        if (hasField(context, "nickname", contact.id)) {
            ops.add(ContentProviderOperation
                    .newUpdate(Data.CONTENT_URI)
                    .withSelection(Data.RAW_CONTACT_ID + "=?" + " AND " + Data.MIMETYPE + " = ?",
                            new String[] {
                                    String.valueOf(contact.id), Nickname.CONTENT_ITEM_TYPE
                            }).withValue(Nickname.NAME, contact.nickname).build());
        } else {
            ops.add(ContentProviderOperation.newInsert(Data.CONTENT_URI)
                    .withValue(Data.RAW_CONTACT_ID, contact.id)
                    .withValue(Data.MIMETYPE, Nickname.CONTENT_ITEM_TYPE)
                    .withValue(Nickname.NAME, contact.nickname).build());
        }
        
        // update photo
        if (contact.shouldUpdatePhoto) {
            if (hasField(context, "photo", contact.id)) {
                if (contact.photo != null) {
                    ops.add(ContentProviderOperation
                            .newUpdate(Data.CONTENT_URI)
                            .withSelection(
                                    Data.RAW_CONTACT_ID + "=?" + " AND " + Data.MIMETYPE + " = ?",
                                    new String[] {
                                            String.valueOf(contact.id), Photo.CONTENT_ITEM_TYPE
                                    }).withValue(Data.DATA15, contact.photo)
                            .withValue(Data.IS_SUPER_PRIMARY, 1).build());
                } else {
                    long dataPhotoId = -1;
                    Uri uri = Data.CONTENT_URI;
                    Cursor cursor = context.getContentResolver().query(uri, new String[] {
                            Data._ID
                    }, Data.CONTENT_TYPE + "=? and" + Data.RAW_CONTACT_ID + "=?", new String[] {
                            Photo.CONTENT_ITEM_TYPE, String.valueOf(contact.id)
                    }, null);
                    if (cursor.moveToNext())
                        dataPhotoId = cursor.getLong(cursor.getColumnIndex(Data._ID));
                    cursor.close();
                    if (-1 != dataPhotoId) {
                        ops.add(ContentProviderOperation.newDelete(
                                ContentUris.withAppendedId(Data.CONTENT_URI, dataPhotoId)).build());
                    }
                }
            } else {
                if (contact.photo != null) {
                    ops.add(ContentProviderOperation
                            .newInsert(Data.CONTENT_URI)
                            .withValue(Data.RAW_CONTACT_ID, contact.id)
                            .withValue(Data.MIMETYPE, Photo.CONTENT_ITEM_TYPE)
                            .withValue(Photo.PHOTO, contact.photo).build());
                }
            }
        }
        
        // update Group
        if (contact.groupInfos != null) {

            // first delete all groups
            ops.add(ContentProviderOperation
                    .newDelete(Data.CONTENT_URI)
                    .withSelection(
                            Data.RAW_CONTACT_ID + "=? AND " + Data.MIMETYPE + "=?",
                            new String[] {
                                    String.valueOf(contact.id),
                                    GroupMembership.CONTENT_ITEM_TYPE
                            })
                    .build());

            for (GroupInfo gi : contact.groupInfos) {
                ops.add(ContentProviderOperation
                        .newInsert(Data.CONTENT_URI)
                        .withValue(Data.RAW_CONTACT_ID, contact.id)
                        .withValue(Data.MIMETYPE, GroupMembership.CONTENT_ITEM_TYPE)
                        .withValue(Data.DATA1, gi.grId).build());
            }
        }

        // update Phone
		if (contact.phoneInfos != null) {
		    
            // first delete all phones
            ops.add(ContentProviderOperation
                    .newDelete(Data.CONTENT_URI)
                    .withSelection(Data.RAW_CONTACT_ID + "=? AND " + Data.MIMETYPE + "=?", new String[] {
                            String.valueOf(contact.id), Phone.CONTENT_ITEM_TYPE
                    })
                    .build());

			for (PhoneInfo pr : contact.phoneInfos) {
					ops.add(ContentProviderOperation
							.newInsert(Data.CONTENT_URI)
							.withValue(Data.RAW_CONTACT_ID, contact.id)
							.withValue(Data.MIMETYPE, Phone.CONTENT_ITEM_TYPE)
							.withValue(Phone.NUMBER, pr.number)
							.withValue(Phone.TYPE, pr.type)
							.withValue(Phone.DATA3, pr.customName).build());
				}
		}
    
        // update Email
        if (contact.emailInfos != null) {

            // first delete all email
            ops.add(ContentProviderOperation
                    .newDelete(Data.CONTENT_URI)
                    .withSelection(Data.RAW_CONTACT_ID + "=? AND " + Data.MIMETYPE + "=?", new String[] {
                            String.valueOf(contact.id), Email.CONTENT_ITEM_TYPE
                    })
                    .build());

            for (EmailInfo er : contact.emailInfos) {
                ops.add(ContentProviderOperation
                        .newInsert(Data.CONTENT_URI)
                        .withValue(Data.RAW_CONTACT_ID, contact.id)
                        .withValue(Data.MIMETYPE, Email.CONTENT_ITEM_TYPE)
                        .withValue(Email.ADDRESS, er.email)
                        .withValue(Email.TYPE, er.type)
                        .withValue(Email.DATA3, er.customName).build());
            }
        }

        // update Organization
		if (contact.orgInfos != null) {
		    
		 // first delete all organizations
            ops.add(ContentProviderOperation
                    .newDelete(Data.CONTENT_URI)
                    .withSelection(Data.RAW_CONTACT_ID + "=? AND " + Data.MIMETYPE + "=?", new String[] {
                            String.valueOf(contact.id), Organization.CONTENT_ITEM_TYPE
                    })
                    .build());
		
            for (OrgInfo or : contact.orgInfos) {
                ops.add(ContentProviderOperation
                        .newInsert(Data.CONTENT_URI)
                        .withValue(Data.RAW_CONTACT_ID, contact.id)
                        .withValue(Data.MIMETYPE, Organization.CONTENT_ITEM_TYPE)
                        .withValue(Organization.COMPANY, or.company)
                        .withValue(Organization.TYPE, or.type)
                        .withValue(Organization.DATA3, or.customName).build());
            }
		}
		
        // update Address
        if (contact.addressInfos != null) {
            // first delete all addresses
            ops.add(ContentProviderOperation
                    .newDelete(Data.CONTENT_URI)
                    .withSelection(Data.RAW_CONTACT_ID + "=? AND " + Data.MIMETYPE + "=?", new String[] {
                            String.valueOf(contact.id), StructuredPostal.CONTENT_ITEM_TYPE
                    })
                    .build());

            for (AddressInfo ar : contact.addressInfos) {
                ops.add(ContentProviderOperation
                        .newInsert(Data.CONTENT_URI)
                        .withValue(Data.RAW_CONTACT_ID, contact.id)
                        .withValue(Data.MIMETYPE, StructuredPostal.CONTENT_ITEM_TYPE)
                        .withValue(Data.DATA3, ar.customName)
                        .withValue(StructuredPostal.COUNTRY, ar.country)
                        .withValue(StructuredPostal.CITY, ar.city)
                        .withValue(StructuredPostal.STREET, ar.street)
                        .withValue(StructuredPostal.POSTCODE, ar.postcode)
                        .withValue(StructuredPostal.REGION, ar.province)
                        .withValue(StructuredPostal.FORMATTED_ADDRESS, ar.address)
                        .withValue(StructuredPostal.TYPE, ar.type).build());
            }
        }
        
        // update IM
        if (contact.imInfos != null) {
            // first delete all im
            ops.add(ContentProviderOperation
                    .newDelete(Data.CONTENT_URI)
                    .withSelection(Data.RAW_CONTACT_ID + "=? AND " + Data.MIMETYPE + "=?", new String[] {
                            String.valueOf(contact.id), Im.CONTENT_ITEM_TYPE
                    })
                    .build());

            for (ImInfo ir : contact.imInfos) {
                ops.add(ContentProviderOperation
                        .newInsert(Data.CONTENT_URI)
                        .withValue(Data.RAW_CONTACT_ID, contact.id)
                        .withValue(Data.MIMETYPE, Im.CONTENT_ITEM_TYPE)
                        .withValue(Im.DATA1, ir.account)
                        .withValue(Im.TYPE, ir.type)
                        .withValue(Data.DATA3, ir.customName).build());
            }
        }

        try {
            
            if (DUMP_PARAMS) {
                Slog.d(ops.toString());
            }

            context.getContentResolver().applyBatch(ContactsContract.AUTHORITY, ops);
            
            Slog.d("updateContactForce X");
            
            return true;
        } catch (RemoteException e) {
            Slog.e("Error updateContactForce", e);
            Slog.d("updateContactForce X");
            return false;
        } catch (OperationApplicationException e) {
            Slog.e("Error updateContactForce", e);
            Slog.d("updateContactForce X");
            return false;
        }
    }

    /**
     * update contact
     * 
     * @param context
     * @param contact
     * @return
     */
    public static boolean updateContact(Context context, Contact contact) {
        Slog.d("updateContact E");

        if (DUMP_PARAMS) {
            Slog.d(">>>>> DUMP CONTACT >>>>>");
            Slog.d(contact.toString());
            Slog.d("<<<<< DUMP CONTACT <<<<<");
        }

        ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();
        
        // update Name
        if (hasField(context, "name", contact.id)) {
            ops.add(ContentProviderOperation
                    .newUpdate(ContactsContract.Data.CONTENT_URI)
                    .withSelection(Data.RAW_CONTACT_ID + "=?" + " AND " + ContactsContract.Data.MIMETYPE + " = ?",
                            new String[] {
                                    String.valueOf(contact.id), StructuredName.CONTENT_ITEM_TYPE
                            }).withValue(StructuredName.DISPLAY_NAME, contact.name).build());
        } else {
            ops.add(ContentProviderOperation
                    .newInsert(ContactsContract.Data.CONTENT_URI)
                    .withValue(ContactsContract.Data.RAW_CONTACT_ID, contact.id)
                    .withValue(ContactsContract.Data.MIMETYPE,
                            ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
                    .withValue(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, contact.name)
                    .withValue(ContactsContract.CommonDataKinds.StructuredName.FAMILY_NAME, "")
                    .withValue(ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME, "")
                    .withValue(ContactsContract.CommonDataKinds.StructuredName.MIDDLE_NAME, "").build());
        }
        
        // update nickname
        if (hasField(context, "nickname", contact.id)) {
            ops.add(ContentProviderOperation
                    .newUpdate(ContactsContract.Data.CONTENT_URI)
                    .withSelection(Data.RAW_CONTACT_ID + "=?" + " AND " + ContactsContract.Data.MIMETYPE + " = ?",
                            new String[] {
                                    String.valueOf(contact.id), Nickname.CONTENT_ITEM_TYPE
                            }).withValue(Nickname.NAME, contact.nickname).build());
        } else {
            ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                    .withValue(ContactsContract.Data.RAW_CONTACT_ID, contact.id)
                    .withValue(ContactsContract.Data.MIMETYPE, Nickname.CONTENT_ITEM_TYPE)
                    .withValue(ContactsContract.CommonDataKinds.Nickname.NAME, contact.nickname).build());
        }
        
        // update photo
        if (contact.shouldUpdatePhoto) {
            if (hasField(context, "photo", contact.id)) {
                if (contact.photo != null) {
                    ops.add(ContentProviderOperation
                            .newUpdate(ContactsContract.Data.CONTENT_URI)
                            .withSelection(
                                    Data.RAW_CONTACT_ID + "=?" + " AND " + ContactsContract.Data.MIMETYPE + " = ?",
                                    new String[] {
                                            String.valueOf(contact.id), Photo.CONTENT_ITEM_TYPE
                                    }).withValue(Data.DATA15, contact.photo)
                            .withValue(ContactsContract.Data.IS_SUPER_PRIMARY, 1).build());
                } else {
                    long dataPhotoId = -1;
                    Uri uri = Data.CONTENT_URI;
                    Cursor cursor = context.getContentResolver().query(uri, new String[] {
                            Data._ID
                    }, Data.CONTENT_TYPE + "=? and" + Data.RAW_CONTACT_ID + "=?", new String[] {
                            Photo.CONTENT_ITEM_TYPE, String.valueOf(contact.id)
                    }, null);
                    if (cursor.moveToNext())
                        dataPhotoId = cursor.getLong(cursor.getColumnIndex(Data._ID));
                    cursor.close();
                    if (-1 != dataPhotoId) {
                        ops.add(ContentProviderOperation.newDelete(
                                ContentUris.withAppendedId(Data.CONTENT_URI, dataPhotoId)).build());
                    }
                }
            } else {
                if (contact.photo != null) {
                    ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                            .withValue(ContactsContract.Data.RAW_CONTACT_ID, contact.id)
                            .withValue(ContactsContract.Data.MIMETYPE, Photo.CONTENT_ITEM_TYPE)
                            .withValue(ContactsContract.CommonDataKinds.Photo.PHOTO, contact.photo).build());
                }
            }
        }
        
        // has group
        if (contact.groupInfos != null) {
            for (int i = 0; i < contact.groupInfos.size(); i++) {
                GroupInfo gi = new GroupInfo();
                gi = contact.groupInfos.get(i);
                if (gi.modifyFlag == ModifyTag.add) {
                    ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                            .withValue(ContactsContract.Data.RAW_CONTACT_ID, contact.id)
                            .withValue(ContactsContract.Data.MIMETYPE, GroupMembership.CONTENT_ITEM_TYPE)
                            .withValue(Data.DATA1, gi.grId).build());
                } else if (gi.modifyFlag == ModifyTag.del) {
                    ops.add(ContentProviderOperation.newDelete(ContentUris.withAppendedId(Data.CONTENT_URI, gi.dataId))
                            .build());
                } else if (gi.modifyFlag == ModifyTag.edit) {
                    ops.add(ContentProviderOperation.newUpdate(ContactsContract.Data.CONTENT_URI)
                            .withSelection(Data._ID + "=?", new String[] {
                                    String.valueOf(gi.dataId)
                            }).withValue(Data.DATA1, gi.grId).build());
                }
            }
        }

        // update Phone
        for (Iterator<PhoneInfo> iter = contact.phoneInfos.iterator(); iter.hasNext();) {
            PhoneInfo pr = iter.next();
            if (pr.modifyFlag == ModifyTag.add) {
                ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                        .withValue(ContactsContract.Data.RAW_CONTACT_ID, contact.id)
                        .withValue(ContactsContract.Data.MIMETYPE, Phone.CONTENT_ITEM_TYPE)
                        .withValue(Phone.NUMBER, pr.number).withValue(Phone.TYPE, pr.type)
                        .withValue(Phone.DATA3, pr.customName).build());
            } else if (pr.modifyFlag == ModifyTag.del) {
                ops.add(ContentProviderOperation.newDelete(ContentUris.withAppendedId(Data.CONTENT_URI, pr.id)).build());
            } else if (pr.modifyFlag == ModifyTag.edit) {
                ops.add(ContentProviderOperation.newUpdate(ContactsContract.Data.CONTENT_URI)
                        .withSelection(Data._ID + "=?", new String[] {
                                String.valueOf(pr.id)
                        }).withValue(Phone.NUMBER, pr.number).withValue(Phone.DATA3, pr.customName)
                        .withValue(Phone.TYPE, pr.type).build());
            }
        }
        // update Email
        for (Iterator<EmailInfo> iter = contact.emailInfos.iterator(); iter.hasNext();) {
            EmailInfo er = iter.next();
            if (er.modifyFlag == ModifyTag.add) {
                ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                        .withValue(ContactsContract.Data.RAW_CONTACT_ID, contact.id)
                        .withValue(ContactsContract.Data.MIMETYPE, Email.CONTENT_ITEM_TYPE)
                        .withValue(Email.ADDRESS, er.email).withValue(Email.TYPE, er.type)
                        .withValue(Email.DATA3, er.customName).build());
            } else if (er.modifyFlag == ModifyTag.del) {
                ops.add(ContentProviderOperation.newDelete(ContentUris.withAppendedId(Data.CONTENT_URI, er.id)).build());
            } else if (er.modifyFlag == ModifyTag.edit) {
                ops.add(ContentProviderOperation.newUpdate(ContactsContract.Data.CONTENT_URI)
                        .withSelection(Data._ID + "=?", new String[] {
                                String.valueOf(er.id)
                        }).withValue(Email.ADDRESS, er.email).withValue(Email.DATA3, er.customName)
                        .withValue(Email.TYPE, er.type).build());
            }
        }

        // update Organization
        for (Iterator<OrgInfo> iter = contact.orgInfos.iterator(); iter.hasNext();) {
            OrgInfo or = iter.next();
            if (or.modifyFlag == ModifyTag.add) {
                ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                        .withValue(ContactsContract.Data.RAW_CONTACT_ID, contact.id)
                        .withValue(ContactsContract.Data.MIMETYPE, Organization.CONTENT_ITEM_TYPE)
                        .withValue(Organization.COMPANY, or.company).withValue(Organization.TYPE, or.type)
                        .withValue(Organization.DATA3, or.customName).build());
            } else if (or.modifyFlag == ModifyTag.del) {
                ops.add(ContentProviderOperation.newDelete(ContentUris.withAppendedId(Data.CONTENT_URI, or.id)).build());
            } else if (or.modifyFlag == ModifyTag.edit) {
                ops.add(ContentProviderOperation.newUpdate(ContactsContract.Data.CONTENT_URI)
                        .withSelection(Data._ID + "=?", new String[] {
                                String.valueOf(or.id)
                        }).withValue(Organization.COMPANY, or.company).withValue(Organization.TYPE, or.type)
                        .withValue(Organization.DATA3, or.customName).build());
            }
        }
        // update Address
        for (Iterator<AddressInfo> iter = contact.addressInfos.iterator(); iter.hasNext();) {
            AddressInfo ar = iter.next();
            if (ar.modifyFlag == ModifyTag.add) {
                ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                        .withValue(ContactsContract.Data.RAW_CONTACT_ID, contact.id)
                        .withValue(ContactsContract.Data.MIMETYPE, StructuredPostal.CONTENT_ITEM_TYPE)
                        .withValue(StructuredPostal.COUNTRY, ar.country).withValue(StructuredPostal.CITY, ar.city)
                        .withValue(StructuredPostal.STREET, ar.street)
                        .withValue(StructuredPostal.POSTCODE, ar.postcode)
                        .withValue(StructuredPostal.REGION, ar.province)
                        .withValue(StructuredPostal.FORMATTED_ADDRESS, ar.address).withValue(Data.DATA3, ar.customName)
                        .withValue(StructuredPostal.TYPE, ar.type).build());

            } else if (ar.modifyFlag == ModifyTag.del) {
                ops.add(ContentProviderOperation.newDelete(ContentUris.withAppendedId(Data.CONTENT_URI, ar.id)).build());
            } else if (ar.modifyFlag == ModifyTag.edit) {
                ops.add(ContentProviderOperation.newUpdate(ContactsContract.Data.CONTENT_URI)
                        .withSelection(Data._ID + "=?", new String[] {
                                String.valueOf(ar.id)
                        }).withValue(StructuredPostal.FORMATTED_ADDRESS, ar.address)
                        .withValue(StructuredPostal.COUNTRY, ar.country).withValue(StructuredPostal.CITY, ar.city)
                        .withValue(StructuredPostal.STREET, ar.street)
                        .withValue(StructuredPostal.POSTCODE, ar.postcode)
                        .withValue(StructuredPostal.DATA3, ar.customName)
                        .withValue(StructuredPostal.REGION, ar.province).withValue(StructuredPostal.TYPE, ar.type)
                        .build());
            }
        }
        // update InfoM
        for (Iterator<ImInfo> iter = contact.imInfos.iterator(); iter.hasNext();) {
            ImInfo ir = iter.next();
            if (ir.modifyFlag == ModifyTag.add) {
                ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                        .withValue(ContactsContract.Data.RAW_CONTACT_ID, contact.id)
                        .withValue(ContactsContract.Data.MIMETYPE, Im.CONTENT_ITEM_TYPE)
                        .withValue(Im.DATA1, ir.account)
                        .withValue(Im.TYPE, ir.type).withValue(Data.DATA3, ir.customName).build());
            } else if (ir.modifyFlag == ModifyTag.del) {
                ops.add(ContentProviderOperation.newDelete(ContentUris.withAppendedId(Data.CONTENT_URI, ir.id)).build());
            } else if (ir.modifyFlag == ModifyTag.edit) {
                ops.add(ContentProviderOperation.newUpdate(ContactsContract.Data.CONTENT_URI)
                        .withSelection(Data._ID + "=?", new String[] {
                                String.valueOf(ir.id)
                        }).withValue(Im.DATA1, ir.account).withValue(Im.TYPE, ir.type)
                        .withValue(Im.DATA3, ir.customName)
                        .build());
            }
        }

        try {
            Slog.d(ops.toString());

            context.getContentResolver().applyBatch(ContactsContract.AUTHORITY, ops);
            return true;
        } catch (RemoteException e) {
            Slog.e("Error updateContact", e);
            return false;
        } catch (OperationApplicationException e) {
            Slog.e("Error updateContact", e);
            return false;
        }
    }

    /**
     * add a contact
     * 
     * @param context
     * @param contact
     * @param AccountInfo
     * @return the id of the new created contact, or -1 if failed
     */
    public static long addContact(Context context, Contact contact) {
        if (DUMP_PARAMS) {
            Slog.d("+++++ DUMP CONTACT +++++");
            
            Slog.d(contact.toString());
            
            Slog.d("----- DUMP CONTACT -----");
        }
        
        ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();
        
        // if the contact's account info is empty, we add the contact to default account
        if (TextUtils.isEmpty(contact.accountInfo.accountName)) {
            contact.accountInfo.accountName = DEFAULT_ACCOUNT_NAME;
            contact.accountInfo.accountType = DEFAULT_ACCOUNT_TYPE;
        }
        
        // rawcontacts'account
        // don't give c._Id value because it is automatically increased
        ops.add(ContentProviderOperation
                .newInsert(ContactsContract.RawContacts.CONTENT_URI)
                .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, contact.accountInfo.accountType)
                .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, contact.accountInfo.accountName)
                .withValue(ContactsContract.RawContacts.AGGREGATION_MODE,
                        ContactsContract.RawContacts.AGGREGATION_MODE_DISABLED).build());

        // add group
        if (contact.groupInfos != null) {
            for (int i = 0; i < contact.groupInfos.size(); i++) {
                GroupInfo gr = contact.groupInfos.get(i);
                ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                        .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                        .withValue(ContactsContract.Data.MIMETYPE, GroupMembership.CONTENT_ITEM_TYPE)
                        .withValue(Data.DATA1, gr.grId).build());
            }
        }

        if (contact.photo != null) {
            ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                    .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                    .withValue(ContactsContract.Data.IS_SUPER_PRIMARY, 1)
                    .withValue(ContactsContract.Data.MIMETYPE, Photo.CONTENT_ITEM_TYPE)
                    .withValue(Photo.PHOTO, contact.photo)
                    .build());
        }
        // name
        if (!contact.name.equals("")) {
            ops.add(ContentProviderOperation
                    .newInsert(ContactsContract.Data.CONTENT_URI)
                    .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                    .withValue(ContactsContract.Data.MIMETYPE,
                            ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
                    .withValue(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, contact.name).build());
        }
        // Organization
        if (contact.orgInfos.size() > 0) {
            for (Iterator<OrgInfo> iter = contact.orgInfos.iterator(); iter.hasNext();) {
                OrgInfo or = iter.next();
                ops.add(ContentProviderOperation
                        .newInsert(ContactsContract.Data.CONTENT_URI)
                        .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                        .withValue(ContactsContract.Data.MIMETYPE,
                                ContactsContract.CommonDataKinds.Organization.CONTENT_ITEM_TYPE)
                        .withValue(ContactsContract.CommonDataKinds.Organization.COMPANY, or.company)
                        .withValue(ContactsContract.CommonDataKinds.Organization.TYPE, or.type)
                        .withValue(ContactsContract.CommonDataKinds.Organization.DATA3, or.customName).build());
            }
        }
        // phone number
        if (contact.phoneInfos.size() > 0) {
            for (Iterator<PhoneInfo> iter = contact.phoneInfos.iterator(); iter.hasNext();) {
                PhoneInfo pr = iter.next();
                ops.add(ContentProviderOperation
                        .newInsert(ContactsContract.Data.CONTENT_URI)
                        .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                        .withValue(ContactsContract.Data.MIMETYPE,
                                ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
                        .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, pr.number)
                        .withValue(ContactsContract.CommonDataKinds.Phone.TYPE, pr.type)
                        .withValue(ContactsContract.CommonDataKinds.Phone.DATA3, pr.customName).build());
            }
        }
        // email
        if (contact.emailInfos.size() > 0) {
            for (Iterator<EmailInfo> iter = contact.emailInfos.iterator(); iter.hasNext();) {
                EmailInfo er = iter.next();
                ops.add(ContentProviderOperation
                        .newInsert(ContactsContract.Data.CONTENT_URI)
                        .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                        .withValue(ContactsContract.Data.MIMETYPE,
                                ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE)
                        .withValue(ContactsContract.CommonDataKinds.Email.DATA, er.email)
                        .withValue(ContactsContract.CommonDataKinds.Email.TYPE, er.type)
                        .withValue(ContactsContract.CommonDataKinds.Email.DATA3, er.customName).build());
            }
        }
        // address
        if (contact.addressInfos.size() > 0) {
            for (Iterator<AddressInfo> iter = contact.addressInfos.iterator(); iter.hasNext();) {
                AddressInfo ar = iter.next();
                ops.add(ContentProviderOperation
                        .newInsert(ContactsContract.Data.CONTENT_URI)
                        .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                        .withValue(ContactsContract.Data.MIMETYPE,
                                ContactsContract.CommonDataKinds.StructuredPostal.CONTENT_ITEM_TYPE)
                        .withValue(ContactsContract.CommonDataKinds.StructuredPostal.TYPE, ar.type)
                        .withValue(ContactsContract.CommonDataKinds.StructuredPostal.COUNTRY, ar.country)
                        .withValue(ContactsContract.CommonDataKinds.StructuredPostal.CITY, ar.city)
                        .withValue(ContactsContract.CommonDataKinds.StructuredPostal.STREET, ar.street)
                        .withValue(ContactsContract.CommonDataKinds.StructuredPostal.REGION, ar.province)
                        .withValue(ContactsContract.CommonDataKinds.StructuredPostal.POSTCODE, ar.postcode)
                        .withValue(ContactsContract.CommonDataKinds.StructuredPostal.FORMATTED_ADDRESS, ar.address)
                        .withValue(ContactsContract.CommonDataKinds.StructuredPostal.DATA3, ar.customName).build());
            }
        }
        // IM
        if (contact.imInfos.size() > 0) {
            for (Iterator<ImInfo> iter = contact.imInfos.iterator(); iter.hasNext();) {
                ImInfo ir = iter.next();
                ops.add(ContentProviderOperation
                        .newInsert(ContactsContract.Data.CONTENT_URI)
                        .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                        .withValue(ContactsContract.Data.MIMETYPE,
                                ContactsContract.CommonDataKinds.Im.CONTENT_ITEM_TYPE)
                        .withValue(ContactsContract.CommonDataKinds.Im.DATA1, ir.account)
                        .withValue(ContactsContract.CommonDataKinds.Im.PROTOCOL, ir.type)
                        .withValue(ContactsContract.CommonDataKinds.Im.DATA3, ir.customName).build());
            }
        }

        // nick name
        if (contact.nickname != null && !"".equals(contact.nickname)) {
            ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                    .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                    .withValue(ContactsContract.Data.MIMETYPE, Nickname.CONTENT_ITEM_TYPE)
                    .withValue(ContactsContract.CommonDataKinds.Nickname.NAME, contact.nickname).build());
        }

        ContentProviderResult[] results;
        try {
            results = context.getContentResolver().applyBatch(ContactsContract.AUTHORITY, ops);
            // insert RawContact result
            ContentProviderResult result = results[0];
            Slog.d("count = " + result.count + ", uri = " + result.uri);

            return ContentUris.parseId(result.uri);
        } catch (RemoteException e) {
            Slog.e("Error when addContact", e);
            return -1;
        } catch (OperationApplicationException e) {
            Slog.e("Error when addContact", e);
            return -1;
        }
    }

    /**
     * get all group's detail information to one account
     * 
     * @param context
     */

    public static List<GroupInfo> getGroupByAccount(Context context, AccountInfo ai) {
        Uri uri = ContactsContract.Groups.CONTENT_URI;
        if (ai == null) {
            ai = new AccountInfo();
            ai.accountName = DEFAULT_ACCOUNT_NAME;
            ai.accountType = DEFAULT_ACCOUNT_TYPE;
        }
        String where = ContactsContract.Groups.ACCOUNT_NAME + "=?" + " and " + ContactsContract.Groups.ACCOUNT_TYPE
                + "=?";
        String whereargs[] = new String[] {
                ai.accountName, ai.accountType
        };
        Cursor cursor = context.getContentResolver().query(uri, null, where, whereargs, null);
        List<GroupInfo> ls = new ArrayList<GroupInfo>();
        if (cursor.moveToFirst()) {
            final int GROUP_ID = cursor.getColumnIndex(Groups._ID);
            final int GROUP_TITLE = cursor.getColumnIndex(Groups.TITLE);
            final int GROUP_ACCOUNT_NAME = cursor.getColumnIndex(Groups.ACCOUNT_NAME);
            final int GROUP_ACCOUNT_TYPE = cursor.getColumnIndex(Groups.ACCOUNT_TYPE);
            do {
                GroupInfo gi = new GroupInfo();
                gi.grId = cursor.getLong(GROUP_ID);
                gi.name = cursor.getString(GROUP_TITLE);
                gi.accountInfo.accountType = cursor.getString(GROUP_ACCOUNT_NAME);
                gi.accountInfo.accountName = cursor.getString(GROUP_ACCOUNT_TYPE);
                ls.add(gi);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return ls;
    }

    /**
     * get all contacts from one account's one group
     * 
     * @param context
     * @param gi
     * @return
     */
    public static List<Contact> getContactsByGroup(Context context, GroupInfo gi) {
        List<Contact> l = new ArrayList<Contact>();
        List<RawContact> lr = new ArrayList<RawContact>();
        List<DataModel> ld = new ArrayList<Contact.DataModel>();
        if (gi == null)
            return l;
        String[] projection = new String[] {
                Data.RAW_CONTACT_ID,
        };
        String where = GroupMembership.GROUP_ROW_ID + "=?" + " and " + Data.MIMETYPE + "=?";
        String whereargs[] = new String[] {
                String.valueOf(gi.grId), GroupMembership.CONTENT_ITEM_TYPE
        };
        Cursor cursor = context.getContentResolver().query(Data.CONTENT_URI, projection, where, whereargs, null);
        while (cursor.moveToNext()) {
            RawContact rw = new RawContact();
            rw.rawId = cursor.getLong(cursor.getColumnIndex(Data.RAW_CONTACT_ID));
            rw.accountInfo.accountName = gi.accountInfo.accountName;
            rw.accountInfo.accountType = gi.accountInfo.accountType;
            lr.add(rw);
        }
        cursor.close();
        ld = convertToDataModels(context, null);
        l = convertToContacts(lr, ld);
        return l;
    }

    /**
     * get all Favorite contacts
     * 
     * @param context
     * @return
     */
    public static List<Contact> getFavoriteContacts(Context context) {
        List<Contact> l = new ArrayList<Contact>();
        List<RawContact> lr = new ArrayList<RawContact>();
        List<DataModel> ld = new ArrayList<Contact.DataModel>();
        String where = RawContacts.STARRED + "=?";
        String whereargs[] = new String[] {
                String.valueOf(CONTACTS_COLLECT_FLAG)
        };
        Cursor cursor = context.getContentResolver().query(RawContacts.CONTENT_URI, new String[] {
                RawContacts._ID, RawContacts.ACCOUNT_NAME, RawContacts.ACCOUNT_TYPE, RawContacts.VERSION
        }, where, whereargs, null);
        while (cursor.moveToNext()) {
            RawContact rw = new RawContact();
            rw.rawId = cursor.getLong(cursor.getColumnIndex(RawContacts._ID));
            rw.accountInfo.accountName = cursor.getString(cursor.getColumnIndex(RawContacts.ACCOUNT_NAME));
            rw.accountInfo.accountType = cursor.getString(cursor.getColumnIndex(RawContacts.ACCOUNT_TYPE));
            rw.version = cursor.getInt(cursor.getColumnIndex(RawContacts.VERSION));
            lr.add(rw);
        }
        cursor.close();
        ld = convertToDataModels(context, null);
        l = convertToContacts(lr, ld);
        return l;
    }

    /**
     * has group
     * 
     * @param context
     * @return
     */
    public static boolean hasGroup(Context context, long rawId) {
        String projection[] = new String[] {
                Data.DATA1
        };
        String where = Data.RAW_CONTACT_ID + "=?" + " and " + Data.MIMETYPE + " = ?";
        String whereargs[] = new String[] {
                String.valueOf(rawId), GroupMembership.CONTENT_ITEM_TYPE
        };
        Cursor cursor = context.getContentResolver().query(Data.CONTENT_URI, projection, where, whereargs, null);
        int k = cursor.getCount();
        cursor.close();
        if (k > 0) {
            return true;
        }
        return false;
    }

    /**
     * @param obj
     * @return
     */
    public static String convertToString(Object obj) {
        if (obj == null) {
            return "";
        }
        return obj.toString();
    }

    /**
     * get photo of a contact
     * 
     * @param contactId
     * @param context
     * @return
     */

    public static byte[] getContactPhoto(Context context, long contactId) {
        Uri contactUri = ContentUris.withAppendedId(Contacts.CONTENT_URI, contactId);
        Uri photoUri = Uri.withAppendedPath(contactUri, Contacts.Photo.CONTENT_DIRECTORY);
        String projection[] = new String[] {
                Photo.PHOTO
        };
        Cursor cursor = context.getContentResolver().query(photoUri, projection, null, null, null);
        try {
            if (cursor.moveToFirst()) {
                byte[] data = cursor.getBlob(cursor.getColumnIndex(Data.DATA15));
                if (data != null) {
                    return data;
                }
            }
        } finally {
            cursor.close();
        }
        return null;
    }

    /**
     * add a group
     */
    public static boolean addGroup(Context context, GroupInfo gi) {
        Uri groupUri = ContactsContract.Groups.CONTENT_URI;
        if (null == gi.accountInfo.accountName || "".equals(gi.accountInfo.accountName)) {
            gi.accountInfo.accountName = DEFAULT_ACCOUNT_NAME;
            gi.accountInfo.accountType = DEFAULT_ACCOUNT_TYPE;
        }
        ContentValues cv = new ContentValues();
        cv.put(Groups.ACCOUNT_NAME, gi.accountInfo.accountName);
        cv.put(Groups.ACCOUNT_TYPE, gi.accountInfo.accountType);
        cv.put(Groups.TITLE, gi.name);
        cv.put(Groups.NOTES, gi.note);
        Uri newUri = context.getContentResolver().insert(groupUri, cv);
        if (Long.parseLong(newUri.getLastPathSegment()) > 0) {
            return true;
        }
        return false;
    }

    /**
     * delete a group
     */
    public static boolean deleteGroup(Context context, long groupId) {
        Uri groupUri = Uri.parse(ContactsContract.Groups.CONTENT_URI.toString() + "?"
                + ContactsContract.CALLER_IS_SYNCADAPTER + "=true");
        System.out.println(groupUri.toString());
        // remeber if we let callerIsSyncAdapter=true we delete the group and
        // contact's data
        // if we don't attach importance to explain callerIsSyncAdapter
        // we only make group's data dirty not really deleted and unless sync
        // contact
        int rows = context.getContentResolver().delete(groupUri, Groups._ID + "=" + groupId, null);
        if (rows > 0) {
            return true;
        }
        return false;
    }

    /**
     * edit a group
     */
    public static boolean updateGroup(Context context, GroupInfo gi) {
        Uri groupUri = ContactsContract.Groups.CONTENT_URI;
        if (null == gi.accountInfo.accountName || "".equals(gi.accountInfo.accountName)) {
            gi.accountInfo.accountName = DEFAULT_ACCOUNT_NAME;
            gi.accountInfo.accountType = DEFAULT_ACCOUNT_TYPE;
        }
        ContentValues cv = new ContentValues();
        cv.put(Groups.ACCOUNT_NAME, gi.accountInfo.accountName);
        cv.put(Groups.ACCOUNT_TYPE, gi.accountInfo.accountType);
        cv.put(Groups.TITLE, gi.name);
        cv.put(Groups.NOTES, gi.note);
        int rows = context.getContentResolver().update(groupUri, cv, Groups._ID + "=?", new String[] {
                String.valueOf(gi.grId)
        });
        if (rows > 0) {
            return true;
        }
        return false;
    }

    /**
     * has the name or nickname or photo
     * 
     * @param filed
     * @param context
     * @param rawId
     * @return
     */
    public static boolean hasField(Context context, String filed, long rawId) {
        Uri dataUri = Data.CONTENT_URI;
        String where = Data.MIMETYPE + "=? and " + Data.RAW_CONTACT_ID + "=?";
        String whereargs[] = null;
        if (filed.equals("name"))
            whereargs = new String[] {
                    StructuredName.CONTENT_ITEM_TYPE, String.valueOf(rawId)
            };
        else if (filed.equals("nickname"))
            whereargs = new String[] {
                    Nickname.CONTENT_ITEM_TYPE, String.valueOf(rawId)
            };
        else if (filed.equals("photo"))
            whereargs = new String[] {
                    Photo.CONTENT_ITEM_TYPE, String.valueOf(rawId)
            };
        String projection[] = new String[] {
                Data._ID
        };
        Cursor cursor = context.getContentResolver().query(dataUri, projection, where, whereargs, null);
        boolean flag = (cursor.getCount() > 0);
        cursor.close();
        return flag;
    }

    /**
     * return List<contact> convert from rawContact and dataModel to Contact
     * 
     * @param List<RawContact>lr
     * @param List<DataModel> ld
     * @return
     */
    public static List<Contact> convertToContacts(List<RawContact> lr, List<DataModel> ld) {
        List<Contact> l = new ArrayList<Contact>();
        if (lr == null || ld == null)
            return l;
        if (lr.size() == 0 || ld.size() == 0)
            return l;
        for (int i = 0; i < lr.size(); i++) {
            RawContact rc = lr.get(i);
            Contact c = new Contact();
            c.id = rc.rawId;
            c.version = rc.version;
            c.accountInfo.accountName = rc.accountInfo.accountName;
            c.accountInfo.accountType = rc.accountInfo.accountType;
            for (int j = 0; j < ld.size(); j++) {
                DataModel d = ld.get(j);
                if (d.rawId == rc.rawId) { // one contact
                    String minetype = d.mimeType;
                    if (minetype.equals(StructuredName.CONTENT_ITEM_TYPE)) {
                        c.name = d.data1;
                    } else if (minetype.equals(Nickname.CONTENT_ITEM_TYPE)) {
                        c.nickname = d.data1;
                    } else if (minetype.equals(Phone.CONTENT_ITEM_TYPE)) {
                        PhoneInfo pr = new PhoneInfo();
                        pr.id = d.dataId;
                        pr.type = d.data2;
                        pr.number = d.data1;
                        if (pr.type == Phone.TYPE_CUSTOM) {
                            pr.customName = d.data3;
                        }
                        c.phoneInfos.add(pr);
                    } else if (minetype.equals(Email.CONTENT_ITEM_TYPE)) {
                        EmailInfo er = new EmailInfo();
                        er.id = d.dataId;
                        er.type = d.data2;
                        er.email = d.data1;
                        if (er.type == Email.TYPE_CUSTOM) {
                            er.customName = d.data3;
                        }
                        c.emailInfos.add(er);
                    } else if (minetype.equals(Im.CONTENT_ITEM_TYPE)) {
                        ImInfo ir = new ImInfo();
                        ir.id = d.dataId;
                        ir.type = d.data5;
                        ir.account = d.data1;
                        if (ir.type == Im.TYPE_CUSTOM) {
                            ir.customName = d.data3;
                        }
                        c.imInfos.add(ir);
                    } else if (minetype.equals(StructuredPostal.CONTENT_ITEM_TYPE)) {
                        AddressInfo ar = new AddressInfo();
                        ar.address = d.data1;
                        ar.id = d.dataId;
                        ar.city = d.data7;
                        ar.country = d.data10;
                        ar.postcode = d.data9;
                        ar.province = d.data8;
                        ar.type = d.data2;
                        if (ar.type == StructuredPostal.TYPE_CUSTOM) {
                            ar.customName = d.data3;
                        }
                        c.addressInfos.add(ar);
                    } else if (minetype.equals(Organization.CONTENT_ITEM_TYPE)) {
                        OrgInfo or = new OrgInfo();
                        or.id = d.dataId;
                        or.type = d.data2;
                        or.company = convertToString(d.data1);
                        if (or.type == Organization.TYPE_CUSTOM) {
                            or.customName = d.data3;
                        }
                        c.orgInfos.add(or);
                    } else if (minetype.equals(Photo.CONTENT_ITEM_TYPE)) {
                        c.photo = d.data15;
                    } else if (minetype.equals(GroupMembership.CONTENT_ITEM_TYPE)) {
                        c.groupInfos.add(d.gi);
                    }
                }
            }
            l.add(c);
        }
        return l;
    }

    /**
     * only for test
     * 
     * @param context
     * @param li
     * @param rawId
     * @return
     */
    public static List<DataModel> convertToDataModels(Context context, List<GroupInfo> li, long rawContactId) {
        List<DataModel> ld = new ArrayList<DataModel>();
        Cursor cursorOfContacts = context.getContentResolver().query(Data.CONTENT_URI, null,
                Data.RAW_CONTACT_ID + "=?", new String[] {
                    String.valueOf(rawContactId)
                }, Data.RAW_CONTACT_ID);
        if (cursorOfContacts == null)
            return ld;
        if (li == null)
            li = new ArrayList<GroupInfo>();
        if (cursorOfContacts.moveToFirst()) {
            final int DATA_MIMETYPE = cursorOfContacts.getColumnIndex(Data.MIMETYPE);
            final int DATA_RAW_ID = cursorOfContacts.getColumnIndex(Data.RAW_CONTACT_ID);
            final int DATA1 = cursorOfContacts.getColumnIndex(Data.DATA1);
            final int DATA_ID = cursorOfContacts.getColumnIndex(Data._ID);
            final int DATA2 = cursorOfContacts.getColumnIndex(Data.DATA2);
            final int DATA3 = cursorOfContacts.getColumnIndex(Data.DATA3);
            final int DATA4 = cursorOfContacts.getColumnIndex(Data.DATA4);
            final int DATA5 = cursorOfContacts.getColumnIndex(Data.DATA5);
            final int DATA7 = cursorOfContacts.getColumnIndex(Data.DATA7);
            final int DATA8 = cursorOfContacts.getColumnIndex(Data.DATA8);
            final int DATA9 = cursorOfContacts.getColumnIndex(Data.DATA9);
            final int DATA10 = cursorOfContacts.getColumnIndex(Data.DATA10);
            final int DATA15 = cursorOfContacts.getColumnIndex(Data.DATA15);
            do {
                DataModel dm = new DataModel();
                String minetype = cursorOfContacts.getString(DATA_MIMETYPE);
                long rawId = cursorOfContacts.getLong(DATA_RAW_ID);
                dm.mimeType = minetype;
                dm.rawId = rawId;
                if (minetype.equals(StructuredName.CONTENT_ITEM_TYPE)) {
                    dm.data1 = cursorOfContacts.getString(DATA1);
                } else if (minetype.equals(Nickname.CONTENT_ITEM_TYPE)) {
                    dm.data1 = cursorOfContacts.getString(DATA1);
                } else if (minetype.equals(Phone.CONTENT_ITEM_TYPE)) {
                    dm.dataId = cursorOfContacts.getLong(DATA_ID);
                    dm.data2 = cursorOfContacts.getInt(DATA2);
                    dm.data1 = cursorOfContacts.getString(DATA1);
                    if (dm.data2 == Phone.TYPE_CUSTOM) {
                        dm.data3 = cursorOfContacts.getString(DATA3);
                    }
                } else if (minetype.equals(Email.CONTENT_ITEM_TYPE)) {
                    dm.dataId = cursorOfContacts.getLong(DATA_ID);
                    dm.data2 = cursorOfContacts.getInt(DATA2);
                    dm.data1 = cursorOfContacts.getString(DATA1);
                    if (dm.data2 == Email.TYPE_CUSTOM) {
                        dm.data3 = cursorOfContacts.getString(DATA3);
                    }
                } else if (minetype.equals(Im.CONTENT_ITEM_TYPE)) {
                    dm.dataId = cursorOfContacts.getLong(DATA_ID);
                    dm.data5 = cursorOfContacts.getInt(DATA5);
                    dm.data1 = cursorOfContacts.getString(DATA1);
                    if (dm.data5 == Im.TYPE_CUSTOM) {
                        dm.data3 = cursorOfContacts.getString(DATA3);
                    }
                } else if (minetype.equals(Organization.CONTENT_ITEM_TYPE)) {
                    dm.dataId = cursorOfContacts.getLong(DATA_ID);
                    dm.data2 = cursorOfContacts.getInt(DATA2);
                    dm.data1 = cursorOfContacts.getString(DATA1);
                    if (dm.data2 == Organization.TYPE_CUSTOM) {
                        dm.data3 = cursorOfContacts.getString(DATA3);
                    }
                } else if (minetype.equals(Photo.CONTENT_ITEM_TYPE)) {
                    dm.data15 = cursorOfContacts.getBlob(DATA15);
                } else if (minetype.equals(StructuredPostal.CONTENT_ITEM_TYPE)) {
                    dm.dataId = cursorOfContacts.getLong(DATA_ID);
                    dm.data1 = cursorOfContacts.getString(DATA1);
                    dm.data4 = cursorOfContacts.getString(DATA4); // street
                    dm.data7 = cursorOfContacts.getString(DATA7); // city
                    dm.data8 = cursorOfContacts.getString(DATA8); // region
                    dm.data9 = cursorOfContacts.getString(DATA9); // postcode
                    dm.data10 = cursorOfContacts.getString(DATA10);// country
                    dm.data2 = cursorOfContacts.getInt(DATA2); // type
                    if (dm.data2 == StructuredPostal.TYPE_CUSTOM) {
                        dm.data3 = cursorOfContacts.getString(DATA3); // customName
                    }
                } else if (minetype.equals(GroupMembership.CONTENT_ITEM_TYPE)) {
                    GroupInfo gr = new GroupInfo();
                    gr.grId = cursorOfContacts.getLong(DATA1);
                    gr.dataId = cursorOfContacts.getLong(DATA_ID);
                    for (int k = 0; k < li.size(); k++) {
                        GroupInfo gi = li.get(k);
                        if (gr.grId == gi.grId) {
                            gr.name = gi.name;
                            break;
                        }
                    }
                    dm.gi = gr;
                }
                ld.add(dm);
            } while (cursorOfContacts.moveToNext());
        }
        cursorOfContacts.close();
        return ld;

    }

    /**
     * return List<DataModel> convert from cursor to DataModel
     * 
     * @param li
     * @param cursorOfContacts
     * @return
     */
    public static List<DataModel> convertToDataModels(Context context, List<GroupInfo> li) {
        List<DataModel> ld = new ArrayList<DataModel>();
        Cursor cursorOfContacts = context.getContentResolver().query(Data.CONTENT_URI, null, null, null,
                Data.RAW_CONTACT_ID);
        if (cursorOfContacts == null)
            return ld;
        if (li == null)
            li = new ArrayList<GroupInfo>();
        if (cursorOfContacts.moveToFirst()) {
            
            final int DATA_MIMETYPE = cursorOfContacts.getColumnIndex(Data.MIMETYPE);
            final int DATA_RAW_ID = cursorOfContacts.getColumnIndex(Data.RAW_CONTACT_ID);
            final int DATA1 = cursorOfContacts.getColumnIndex(Data.DATA1);
            final int DATA_ID = cursorOfContacts.getColumnIndex(Data._ID);
            final int DATA2 = cursorOfContacts.getColumnIndex(Data.DATA2);
            final int DATA3 = cursorOfContacts.getColumnIndex(Data.DATA3);
            final int DATA5 = cursorOfContacts.getColumnIndex(Data.DATA5);
            final int DATA15 = cursorOfContacts.getColumnIndex(Data.DATA15);
            final int DATA4 = cursorOfContacts.getColumnIndex(Data.DATA4);
            final int DATA7 = cursorOfContacts.getColumnIndex(Data.DATA7);
            final int DATA8 = cursorOfContacts.getColumnIndex(Data.DATA8);
            final int DATA9 = cursorOfContacts.getColumnIndex(Data.DATA9);
            final int DATA10 = cursorOfContacts.getColumnIndex(Data.DATA10);
            
            do {
                DataModel dm = new DataModel();
                String mimetype = cursorOfContacts.getString(DATA_MIMETYPE);
                long rawId = cursorOfContacts.getLong(DATA_RAW_ID);
                dm.mimeType = mimetype;
                dm.rawId = rawId;
                if (mimetype.equals(StructuredName.CONTENT_ITEM_TYPE)) {
                    dm.data1 = cursorOfContacts.getString(DATA1);
                } else if (mimetype.equals(Nickname.CONTENT_ITEM_TYPE)) {
                    dm.data1 = cursorOfContacts.getString(DATA1);
                } else if (mimetype.equals(Phone.CONTENT_ITEM_TYPE)) {
                    dm.dataId = cursorOfContacts.getLong(DATA_ID);
                    dm.data2 = cursorOfContacts.getInt(DATA2);
                    dm.data1 = cursorOfContacts.getString(DATA1);
                    if (dm.data2 == Phone.TYPE_CUSTOM) {
                        dm.data3 = cursorOfContacts.getString(DATA3);
                    }
                } else if (mimetype.equals(Email.CONTENT_ITEM_TYPE)) {
                    dm.dataId = cursorOfContacts.getLong(DATA_ID);
                    dm.data2 = cursorOfContacts.getInt(DATA2);
                    dm.data1 = cursorOfContacts.getString(DATA1);
                    if (dm.data2 == Email.TYPE_CUSTOM) {
                        dm.data3 = cursorOfContacts.getString(DATA3);
                    }
                } else if (mimetype.equals(Im.CONTENT_ITEM_TYPE)) {
                    dm.dataId = cursorOfContacts.getLong(DATA_ID);
                    dm.data5 = cursorOfContacts.getInt(DATA5);
                    dm.data1 = cursorOfContacts.getString(DATA1);
                    if (dm.data5 == Im.TYPE_CUSTOM) {
                        dm.data3 = cursorOfContacts.getString(DATA3);
                    }
                } else if (mimetype.equals(Organization.CONTENT_ITEM_TYPE)) {
                    dm.dataId = cursorOfContacts.getLong(DATA_ID);
                    dm.data1 = cursorOfContacts.getString(DATA1);
                    if (cursorOfContacts.isNull(DATA2)) {
                        dm.data2 = Organization.TYPE_WORK;
                    } else {
                        dm.data2 = cursorOfContacts.getInt(DATA2);
                    }
                    if (dm.data2 == Organization.TYPE_CUSTOM) {
                        dm.data3 = cursorOfContacts.getString(DATA3);
                    }
                } else if (mimetype.equals(StructuredPostal.CONTENT_ITEM_TYPE)) {
                    dm.dataId = cursorOfContacts.getLong(DATA_ID);
                    dm.data1 = cursorOfContacts.getString(DATA1);
                    dm.data4 = cursorOfContacts.getString(DATA4); // street
                    dm.data7 = cursorOfContacts.getString(DATA7); // city
                    dm.data8 = cursorOfContacts.getString(DATA8); // region
                    dm.data9 = cursorOfContacts.getString(DATA9); // postcode
                    dm.data10 = cursorOfContacts.getString(DATA10);// country
                    dm.data2 = cursorOfContacts.getInt(DATA2); // type
                    if (dm.data2 == StructuredPostal.TYPE_CUSTOM) {
                        dm.data3 = cursorOfContacts.getString(DATA3); // customName
                    }
                } else if (mimetype.equals(Photo.CONTENT_ITEM_TYPE)) {
                    dm.data15 = cursorOfContacts.getBlob(DATA15);
                } else if (mimetype.equals(GroupMembership.CONTENT_ITEM_TYPE)) {
                    GroupInfo gr = new GroupInfo();
                    gr.grId = cursorOfContacts.getLong(DATA1);
                    gr.dataId = cursorOfContacts.getLong(DATA_ID);
                    for (int k = 0; k < li.size(); k++) {
                        GroupInfo gi = li.get(k);
                        if (gr.grId == gi.grId) {
                            gr.name = gi.name;
                            break;
                        }
                    }
                    dm.gi = gr;
                }
                ld.add(dm);
            } while (cursorOfContacts.moveToNext());
        }
        cursorOfContacts.close();
        return ld;
    }

    /**
     * getallrawcontacts
     * 
     * @param context
     * @return
     */
    public static List<RawContact> getAllRawContacts(Context context) {
        List<RawContact> lr = new ArrayList<RawContact>();
        Cursor cursor = context.getContentResolver().query(RawContacts.CONTENT_URI, new String[] {
                RawContacts._ID, RawContacts.ACCOUNT_NAME, RawContacts.ACCOUNT_TYPE, RawContacts.VERSION
        }, RawContacts.DELETED + "!=" + DELETE_FLAG, null, null);
        while (cursor.moveToNext()) {
            RawContact rw = new RawContact();
            rw.rawId = cursor.getLong(cursor.getColumnIndex(RawContacts._ID));
            rw.accountInfo.accountName = cursor.getString(cursor.getColumnIndex(RawContacts.ACCOUNT_NAME));
            rw.accountInfo.accountType = cursor.getString(cursor.getColumnIndex(RawContacts.ACCOUNT_TYPE));
            rw.version = cursor.getInt(cursor.getColumnIndex(RawContacts.VERSION));
            lr.add(rw);
        }
        cursor.close();
        return lr;
    }

    /**
     * get All contacts The list of original contact
     * 
     * @param context
     * @return
     */
    public static List<Contact> getAllContacts(Context context) {
        List<RawContact> lr = getAllRawContacts(context);
        List<GroupInfo> li = getAllGroups(context);
        List<DataModel> ld = convertToDataModels(context, li);
        return convertToContacts(lr, ld);
    }

    /**
     * only for test get a contact by rawId
     * 
     * @param context
     * @param rawId
     * @return
     */
    public static Contact getContactById(Context context, long rawId) {
        List<RawContact> lr = new ArrayList<Contact.RawContact>();
        Cursor cursor = context.getContentResolver().query(RawContacts.CONTENT_URI, new String[] {
                RawContacts.ACCOUNT_NAME, RawContacts.ACCOUNT_TYPE, RawContacts.VERSION
        }, RawContacts.DELETED + "!=" + DELETE_FLAG + " and " + RawContacts._ID + "=?", new String[] {
                String.valueOf(rawId)
        }, null);
        RawContact rw = new RawContact();
        if (cursor.moveToFirst()) {
            rw.rawId = rawId;
            rw.accountInfo.accountName = cursor.getString(cursor.getColumnIndex(RawContacts.ACCOUNT_NAME));
            rw.accountInfo.accountType = cursor.getString(cursor.getColumnIndex(RawContacts.ACCOUNT_TYPE));
            rw.version = cursor.getInt(cursor.getColumnIndex(RawContacts.VERSION));
        }
        cursor.close();
        lr.add(rw);
        List<GroupInfo> li = getAllGroups(context);
        List<DataModel> ld = convertToDataModels(context, li, rawId);
        return convertToContacts(lr, ld).get(0);
    }

    /**
     * 遍历联系人的方法
     */
    public static void print(Contact c) {
        System.out.println("联系人Id" + c.id);
        System.out.print("联系人姓名" + c.name);
        for (int i = 0; i < c.phoneInfos.size(); i++) {
            PhoneInfo pr = c.phoneInfos.get(i);
            if (pr.type == Phone.TYPE_HOME) {
                System.out.print("电话类型" + "家庭电话   ");
            }
            if (pr.type == Phone.TYPE_OTHER) {
                System.out.print("   电话类型" + "其他电话  ");
            }
            if (pr.type == Phone.TYPE_MOBILE) {
                System.out.println("  电话类型" + "个人电话  ");
            }
            if (pr.type == Phone.TYPE_OTHER_FAX) {
                System.out.print("   电话类型" + " 传真电话  ");
            }
            if (pr.type == Phone.TYPE_WORK) {
                System.out.print("  电话类型" + "  工作电话  ");
            }
            System.out.println("电话号码" + pr.number);
        }
        for (int i = 0; i < c.emailInfos.size(); i++) {
            EmailInfo er = c.emailInfos.get(i);
            if (er.type == Email.TYPE_HOME) {
                System.out.print("邮件类型" + "家庭邮件");
            }
            if (er.type == Email.TYPE_OTHER) {
                System.out.print("邮件类型" + "其他邮件");
            }
            if (er.type == Email.TYPE_WORK) {
                System.out.print("邮件类型" + "工作邮箱");
            }
            System.out.println("邮箱地址" + er.email);
        }

        for (int i = 0; i < c.imInfos.size(); i++) {
            ImInfo im = c.imInfos.get(i);
            if (im.type == Im.PROTOCOL_AIM) {
                System.out.print("通讯工具类型" + "AIM");
            }
            if (im.type == Im.PROTOCOL_GOOGLE_TALK) {
                System.out.print("通讯工具类型" + "PROTOCOL_GOOGLE_TALK");
            }
            if (im.type == Im.PROTOCOL_JABBER) {
                System.out.print("通讯工具类型" + "PROTOCOL_JABBER");
            }
            if (im.type == Im.PROTOCOL_QQ) {
                System.out.print("通讯工具类型" + "QQ");
            }
            if (im.type == Im.PROTOCOL_MSN) {
                System.out.print("通讯工具类型" + "MSN");
            }
            if (im.type == Im.PROTOCOL_YAHOO) {
                System.out.print("通讯工具类型" + "PROTOCOL_YAHOO");
            }
            if (im.type == Im.PROTOCOL_ICQ) {
                System.out.print("通讯工具类型" + "PROTOCOL_ICQ");
            }
            if (im.type == Im.PROTOCOL_SKYPE) {
                System.out.print("通讯工具类型" + "PROTOCOL_SKYPE");
            }
            System.out.println("通讯工具号码" + im.account);
        }
    }
}
